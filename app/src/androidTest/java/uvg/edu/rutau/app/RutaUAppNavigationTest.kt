package uvg.edu.rutau.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Comprueba que las pantallas principales se conecten correctamente. */
@RunWith(AndroidJUnit4::class)
class RutaUAppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var appState: RutaUAppState

    @Before
    fun setUp() {
        RutaUAppDependencies.resetForTesting()
        composeRule.setContent {
            val state = rememberRutaUAppState()
            SideEffect { appState = state }
            RutaUTheme {
                RutaUApp(appState = state)
            }
        }
    }

    @Test
    fun loginNavigatesToTrips() {
        loginWithDefaultAccount()

        assertScreenVisible("Mis trayectos")
    }

    @Test
    fun registrationNavigatesToTrips() {
        composeRule.onNodeWithTag("LoginSignUpButton").performClick()
        composeRule.onNodeWithTag("SignUpFullNameInput").performTextInput("Ana García")

        composeRule.onNodeWithTag("SignUpUniversitySelector").performClick()
        composeRule.onNodeWithText("Universidad de San Carlos de Guatemala").performClick()
        composeRule.onNodeWithTag("SignUpCampusSelector").performClick()
        composeRule.onNodeWithText("Campus Central").performClick()

        composeRule.onNodeWithTag("SignUpEmailInput").performScrollTo().performTextInput("ana@ejemplo.com")
        composeRule.onNodeWithTag("SignUpPasswordInput").performScrollTo().performTextInput("RutaU123")
        composeRule.onNodeWithTag("SignUpConfirmPasswordInput").performScrollTo().performTextInput("RutaU123")
        composeRule.onNodeWithTag("SignUpTermsCheckbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("SignUpSubmitButton").performScrollTo().performClick()

        assertScreenVisible("Mis trayectos")
    }

    @Test
    fun recoveryDemoLinkNavigatesToResetPassword() {
        composeRule.onNodeWithTag("LoginForgotPasswordButton").performClick()
        composeRule.onNodeWithTag("RecoveryEmailInput").performTextInput("mateo@ejemplo.com")
        composeRule.onNodeWithTag("RecoverySubmitButton").performClick()
        assertScreenVisible("Revisa tu correo")

        composeRule.onNodeWithTag("RecoveryOpenDemoLinkButton").performClick()

        assertScreenVisible("Crea una nueva contraseña")
    }

    @Test
    fun logoutClearsNavigationHistory() {
        loginWithDefaultAccount()
        openAccount()

        composeRule.onNodeWithTag("AccountLogoutButton").performScrollTo().performClick()
        composeRule.onNodeWithTag("ConfirmationDialogConfirmButton").performClick()

        assertScreenVisible("Bienvenido a RutaU")
        composeRule.runOnIdle {
            assertFalse(appState.navController.popBackStack())
        }
    }

    @Test
    fun bottomNavigationRestoresAccountFormState() {
        loginWithDefaultAccount()
        openAccount()

        composeRule.onNodeWithTag("AccountFullNameInput").performTextClearance()
        composeRule.onNodeWithTag("AccountFullNameInput").performTextInput("Mateo Temporal")
        composeRule.onNodeWithTag("BottomNavigation-TRIPS").performClick()
        assertScreenVisible("Mis trayectos")
        composeRule.onNodeWithTag("BottomNavigation-ACCOUNT").performClick()

        composeRule.onNodeWithTag("AccountFullNameInput").assertTextEquals("Mateo Temporal")
    }

    @Test
    fun accountChangesAreStoredInTheSharedMock() {
        loginWithDefaultAccount()
        openAccount()

        composeRule.onNodeWithTag("AccountFullNameInput").performTextClearance()
        composeRule.onNodeWithTag("AccountFullNameInput").performTextInput("Mateo Actualizado")
        composeRule.onNodeWithTag("AccountSaveProfileButton").performClick()
        assertScreenVisible("Tus datos fueron actualizados.")

        composeRule.runOnIdle {
            assertEquals("Mateo Actualizado", RutaUAppDependencies.currentUserForTesting()?.fullName)
        }
    }

    private fun loginWithDefaultAccount() {
        composeRule.onNodeWithTag("LoginEmailInput").performTextInput("mateo@ejemplo.com")
        composeRule.onNodeWithTag("LoginPasswordInput").performTextInput("RutaU123")
        composeRule.onNodeWithTag("LoginSubmitButton").performClick()
        assertScreenVisible("Mis trayectos")
    }

    private fun openAccount() {
        composeRule.onNodeWithTag("BottomNavigation-ACCOUNT").performClick()
        assertScreenVisible("Datos personales")
    }

    private fun assertScreenVisible(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
    }
}
