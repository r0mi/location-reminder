package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import androidx.transition.Slide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.PointOfInterest
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.distanceTo
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.toBounds
import org.koin.android.ext.android.inject
import timber.log.Timber

class SelectLocationFragment : BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var poiMarker: Marker? = null
    private var name: String = ""
    private val args: SelectLocationFragmentArgs by navArgs()
    private var currentPOI: PointOfInterest? = null
    private var updateMap = true
    private var showEnableMyLocationMenuItem = true
    private var lastLocationRetries = 0
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val startIntentSenderForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            checkDeviceLocationSettings(false)
        }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showSnackbarWithAction(
                    R.string.fine_location_permission_rationale,
                    R.string.settings
                ) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            } else if (::map.isInitialized) {
                enableMyLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            slideEdge = Gravity.START
        }
        enterTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            slideEdge = Gravity.END
        }
        returnTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            slideEdge = Gravity.END
        }
        currentPOI = args.currentPOI
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap: GoogleMap ->
            map = googleMap
            setMapLongClick(map)
            setPoiClick(map)
            setCircleClick(map)
            setMapStyle(map)
            enableMyLocation()
            _viewModel.listOfLatLngs.value?.let {
                drawPois(it)
            }
            if (currentPOI == null) {
                _viewModel.showSnackBarInt.value = R.string.select_poi
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _viewModel.listOfLatLngs.observe(viewLifecycleOwner) {
            it?.let { pois ->
                if (updateMap) {
                    drawPois(pois)
                }
            }
        }
        name = currentPOI?.name ?: ""
        binding.saveButton.setOnClickListener {
            onLocationSelected(_viewModel.listOfLatLngs.value)
        }
    }

    override fun onStart() {
        super.onStart()
        if (::map.isInitialized) {
            enableMyLocation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.enable_my_location)?.isVisible = showEnableMyLocationMenuItem
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        R.id.clear_map -> {
            clearMap()
            true
        }
        R.id.enable_my_location -> {
            enableMyLocation()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onLocationSelected(pois: List<LatLng>? = null) {
        if (pois.isNullOrEmpty()) {
            _viewModel.showSnackBarInt.value = R.string.select_poi
        } else {
            if (name.isBlank() || pois.size > 2) {
                name = getString(R.string.point_of_interest)
            }
            val poi = when (pois.size) {
                1 -> {
                    val latLng = pois[0]
                    val radius = currentPOI?.let {
                        if (it.latLng == pois[0]) {
                            it.radius
                        } else {
                            POI_RADIUS_IN_METERS
                        }
                    } ?: POI_RADIUS_IN_METERS
                    updateMap = false
                    _viewModel.clearAllPOIData() // Just in case we use a custom radius
                    updateMap = true
                    PointOfInterest(latLng, radius, name)
                }
                2 -> PointOfInterest(pois[0], pois[0].distanceTo(pois[1]), name)
                else -> getCircularBounds(pois).let {
                    PointOfInterest(it.first, it.second, name)
                }
            }
            _viewModel.persistPOIData()
            // Use navigation arguments to send back the selected location because we want to display a snackbar
            // depending on whether we are adding or updating the location and want the coordinator layout animation
            // for the FAB to work
            _viewModel.navigationCommand.value = NavigationCommand.To(
                SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment(
                    poi
                )
            )
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            if (_viewModel.listOfLatLngs.value.isNullOrEmpty() && name.isBlank()) {
                name = poi.name
            }
            _viewModel.addLatLngIfNotInList(poi.latLng)
            poiMarker?.remove()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            AppCompatResources.getDrawable(
                                requireContext(),
                                R.drawable.ic_poi
                            )!!.toBitmap()
                        )
                    )
            )
            poiMarker?.showInfoWindow()
        }
    }

    private fun setCircleClick(map: GoogleMap) {
        map.setOnCircleClickListener {
            clearMap()
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            _viewModel.addLatLngIfNotCloseElseDelete(latLng)
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Timber.e("Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Timber.e(e, "Can't find style. Error: ")
        }
    }

    private fun enableMyLocation() {
        if (!isAdded) {
            return
        }
        if (checkLocationPermission()) {
            map.isMyLocationEnabled = true
            map.uiSettings.isCompassEnabled = true
            checkDeviceLocationSettings()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkLocationPermission() = ActivityCompat.checkSelfPermission(
        requireActivity(),
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    startIntentSenderForResult.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.d("Error getting location settings resolution: %s", sendEx.message)
                }
            } else {
                showSnackbarWithAction(
                    R.string.location_service_rationale_for_select_location_fragment,
                    R.string.enable
                ) {
                    checkDeviceLocationSettings()
                }
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                showEnableMyLocationMenuItem = false

                if (checkLocationPermission()) {
                    if (!_viewModel.listOfLatLngs.value.isNullOrEmpty() && ::map.isInitialized) {
                        _viewModel.listOfLatLngs.value?.let { pois ->
                            val poi = when (pois.size) {
                                1 -> Pair(pois[0], currentPOI?.radius ?: POI_RADIUS_IN_METERS)
                                2 -> Pair(pois[0], pois[0].distanceTo(pois[1]))
                                else -> getCircularBounds(pois)
                            }

                            map.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    poi.first.toBounds(poi.second), 100
                                )
                            )

                        }
                    } else {
                        getLastLocation()
                    }
                } else {
                    requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    private fun getLastLocation() {
        if (checkLocationPermission()) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && ::map.isInitialized) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location.latitude,
                                location.longitude
                            ), 15f
                        )
                    )
                    lastLocationRetries = 0
                } else if (location == null && lastLocationRetries++ < MAXIMUM_LOCATION_RETRIES) {
                    view?.postDelayed({ getLastLocation() }, LOCATION_RETRY_DELAY_MS)
                }
            }
        }
    }

    private fun clearMap() {
        map.clear()
        name = ""
        currentPOI = null
        _viewModel.clearAllPOIData()
    }

    private fun drawPois(pois: List<LatLng>) {
        if (!::map.isInitialized) return
        map.clear()
        if (pois.isEmpty()) {
            currentPOI?.let {
                map.addMarker(
                    MarkerOptions()
                        .position(it.latLng)
                        .title(it.name)
                        .icon(
                            BitmapDescriptorFactory.fromBitmap(
                                AppCompatResources.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_poi
                                )!!.toBitmap()
                            )
                        )
                )
                map.addCircle(
                    CircleOptions()
                        .clickable(true)
                        .center(it.latLng)
                        .radius(it.radius)
                        .fillColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPoiFill
                            )
                        )
                        .strokeColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPoiStroke
                            )
                        )
                        .strokeWidth(2f)
                )
                updateMap = false
                _viewModel.addLatLngIfNotCloseElseDelete(it.latLng)
                updateMap = true
            }
            return
        }
        pois.forEach {
            map.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(getString(R.string.dropped_pin))
                    .snippet(
                        getString(
                            R.string.lat_long_snippet,
                            it.latitude,
                            it.longitude
                        )
                    )
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            AppCompatResources.getDrawable(
                                requireContext(),
                                R.drawable.ic_poi
                            )!!.toBitmap()
                        )
                    )
            )
        }
        val poi = when (pois.size) {
            1 -> Pair(pois[0], POI_RADIUS_IN_METERS)
            2 -> Pair(pois[0], pois[0].distanceTo(pois[1]))
            else -> getCircularBounds(pois)
        }
        map.addCircle(
            CircleOptions()
                .clickable(true)
                .center(poi.first)
                .radius(poi.second)
                .fillColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPoiFill
                    )
                )
                .strokeColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPoiStroke
                    )
                )
                .strokeWidth(2f)
        )
    }

    private fun getCircularBounds(pois: List<LatLng>): Pair<LatLng, Double> {
        val boundsBuilder = LatLngBounds.Builder()
        pois.forEach {
            boundsBuilder.include(it)
        }
        val bounds = boundsBuilder.build()
        return Pair(bounds.center, bounds.center.distanceTo(bounds.southwest))
    }

    companion object {
        private const val MAXIMUM_LOCATION_RETRIES = 10
        private const val LOCATION_RETRY_DELAY_MS = 500L
        private const val POI_RADIUS_IN_METERS = 100.0
        internal const val DELETE_POI_RADIUS_IN_METERS = 30.0
    }
}
