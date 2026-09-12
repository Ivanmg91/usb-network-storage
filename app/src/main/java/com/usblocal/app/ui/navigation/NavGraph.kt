package com.usblocal.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.usblocal.app.ui.browser.FileBrowserScreen
import com.usblocal.app.ui.connections.AddEditConnectionScreen
import com.usblocal.app.ui.connections.ConnectionsScreen

object Routes {
    const val CONNECTIONS = "connections"
    const val ADD_CONNECTION = "add_connection"
    const val EDIT_CONNECTION = "edit_connection/{connectionId}"
    const val FILE_BROWSER = "browser/{connectionId}"
    const val MEDIA_VIEWER = "media_viewer/{connectionId}?path={path}"
    const val TEXT_VIEWER = "text_viewer/{connectionId}?path={path}"

    fun editConnection(connectionId: String) = "edit_connection/$connectionId"
    fun fileBrowser(connectionId: String) = "browser/$connectionId"
    
    fun mediaViewer(connectionId: String, path: String) = 
        "media_viewer/$connectionId?path=${Uri.encode(path)}"
        
    fun textViewer(connectionId: String, path: String) = 
        "text_viewer/$connectionId?path=${Uri.encode(path)}"
}

private val enterTransition: EnterTransition = slideInHorizontally(
    initialOffsetX = { it / 3 },
    animationSpec = tween(300)
) + fadeIn(animationSpec = tween(300))

private val exitTransition: ExitTransition = slideOutHorizontally(
    targetOffsetX = { -it / 3 },
    animationSpec = tween(300)
) + fadeOut(animationSpec = tween(300))

private val popEnterTransition: EnterTransition = slideInHorizontally(
    initialOffsetX = { -it / 3 },
    animationSpec = tween(300)
) + fadeIn(animationSpec = tween(300))

private val popExitTransition: ExitTransition = slideOutHorizontally(
    targetOffsetX = { it / 3 },
    animationSpec = tween(300)
) + fadeOut(animationSpec = tween(300))

@Composable
fun USBLocalNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CONNECTIONS,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition }
    ) {
        composable(Routes.CONNECTIONS) {
            ConnectionsScreen(
                onNavigateToAddConnection = {
                    navController.navigate(Routes.ADD_CONNECTION)
                },
                onNavigateToEditConnection = { id ->
                    navController.navigate(Routes.editConnection(id))
                },
                onNavigateToBrowser = { id ->
                    navController.navigate(Routes.fileBrowser(id))
                }
            )
        }

        composable(Routes.ADD_CONNECTION) {
            AddEditConnectionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_CONNECTION,
            arguments = listOf(navArgument("connectionId") { type = NavType.StringType })
        ) {
            AddEditConnectionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FILE_BROWSER,
            arguments = listOf(navArgument("connectionId") { type = NavType.StringType })
        ) {
            FileBrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMediaViewer = { id, path ->
                    navController.navigate(Routes.mediaViewer(id, path))
                },
                onNavigateToTextViewer = { id, path ->
                    navController.navigate(Routes.textViewer(id, path))
                }
            )
        }
        
        composable(
            route = Routes.MEDIA_VIEWER,
            arguments = listOf(
                navArgument("connectionId") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; nullable = true }
            )
        ) {
            // We will create MediaViewerScreen
            com.usblocal.app.ui.viewer.MediaViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.TEXT_VIEWER,
            arguments = listOf(
                navArgument("connectionId") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; nullable = true }
            )
        ) {
            // We will create TextViewerScreen
            com.usblocal.app.ui.viewer.TextViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
