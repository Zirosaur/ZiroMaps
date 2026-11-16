package com.zirosaur.ziromaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zirosaur.ziromaps.ui.camera.CameraStampScreen
import com.zirosaur.ziromaps.ui.library.MapLibraryScreen
import com.zirosaur.ziromaps.ui.viewer.MapViewerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "map_library"
                    ) {
                        composable("map_library") {
                            MapLibraryScreen(
                                onOpenMap = { projectId ->
                                    navController.navigate("map_viewer/$projectId")
                                }
                            )
                        }
                        composable(
                            route = "map_viewer/{projectId}",
                            arguments = listOf(
                                navArgument("projectId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
                            MapViewerScreen(
                                projectId = projectId,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenCameraStamp = { id -> navController.navigate("camera_stamp/$id") }
                            )
                        }
                        composable(
                            route = "camera_stamp/{projectId}",
                            arguments = listOf(
                                navArgument("projectId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
                            CameraStampScreen(
                                projectId = projectId,
                                onNavigateBack = { navController.popBackStack() },
                                onPhotoSaved = { _ -> navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
