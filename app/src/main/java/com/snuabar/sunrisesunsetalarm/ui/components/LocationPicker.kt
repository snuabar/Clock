package com.snuabar.sunrisesunsetalarm.ui.components

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.snuabar.sunrisesunsetalarm.R
import com.snuabar.sunrisesunsetalarm.data.City
import com.snuabar.sunrisesunsetalarm.data.CityData
import com.snuabar.sunrisesunsetalarm.util.LocationManagerHelper
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPicker(
    onCitySelected: (City) -> Unit,
    onDismiss: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationManagerHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var filteredCities by remember { mutableStateOf(CityData.cities) }
    var geocoderResults by remember { mutableStateOf<List<City>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    // Search with debounce using Geocoder
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            filteredCities = CityData.cities
            geocoderResults = emptyList()
            return@LaunchedEffect
        }

        // Local search
        filteredCities = CityData.searchCities(searchQuery)

        // Geocoder search for broader results
        isSearching = true
        val results = withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(searchQuery, 10)
                addresses?.mapNotNull { address ->
                    // Build a descriptive name using available address components
                    val adminArea = address.adminArea
                    val locality = address.locality
                    val subAdminArea = address.subAdminArea
                    val featureName = address.featureName

                    // Build full address name for display
                    val parts = mutableListOf<String>()
                    adminArea?.let { parts.add(it) }
                    locality?.let { if (!parts.contains(it)) parts.add(it) }
                    subAdminArea?.let { if (!parts.contains(it)) parts.add(it) }
                    featureName?.let { if (!parts.contains(it)) parts.add(it) }

                    val displayName = if (parts.isEmpty()) {
                        address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName ?: return@mapNotNull null
                    } else {
                        parts.joinToString(" · ")
                    }

                    City(
                        id = "geo_${System.currentTimeMillis()}_${address.latitude}",
                        name = displayName,
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
        geocoderResults = results.filter { geoCity ->
            // Filter out duplicates from local results
            // Check if any local city name is contained in the geocoder result name
            filteredCities.none { localCity ->
                geoCity.name.contains(localCity.name) || localCity.name.contains(geoCity.name)
            }
        }
        isSearching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.select_city),
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.search_city)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GPS option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isLocating = true
                        onRequestLocationPermission()
                        coroutineScope.launch {
                            val location = locationHelper.getCurrentLocation()
                            location?.let { loc ->
                                val cityName = withContext(Dispatchers.IO) {
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                        addresses?.firstOrNull()?.let { address ->
                                            address.locality
                                                ?: address.subAdminArea
                                                ?: address.adminArea
                                                ?: context.getString(R.string.current_location)
                                        } ?: context.getString(R.string.current_location)
                                    } catch (_: Exception) {
                                        context.getString(R.string.current_location)
                                    }
                                }
                                val city = City(
                                    id = "gps_${System.currentTimeMillis()}",
                                    name = cityName,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude
                                )
                                onCitySelected(city)
                            }
                            isLocating = false
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (isLocating) stringResource(R.string.locating) else stringResource(R.string.use_current_location))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if ((filteredCities.isEmpty() && geocoderResults.isEmpty()) && !isSearching) {
                Text(
                    text = stringResource(R.string.no_matching_city),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    if (searchQuery.isNotBlank() && filteredCities.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.local_data),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(filteredCities, key = { it.id }) { city ->
                            CityItem(city = city, onClick = { onCitySelected(city) })
                        }
                    }
                    if (searchQuery.isNotBlank() && geocoderResults.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.network_search),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(geocoderResults, key = { it.id }) { city ->
                            CityItem(city = city, onClick = { onCitySelected(city) })
                        }
                    }
                    if (searchQuery.isBlank()) {
                        items(filteredCities, key = { it.id }) { city ->
                            CityItem(city = city, onClick = { onCitySelected(city) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityItem(city: City, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = city.name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
