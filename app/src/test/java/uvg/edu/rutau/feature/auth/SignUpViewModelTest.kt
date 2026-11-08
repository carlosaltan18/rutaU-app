package uvg.edu.rutau.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.data.repository.FakeSessionRepository

/** Comprueba la validación inmediata del formulario de registro. */
class SignUpViewModelTest {
    @Test
    fun `updates each field error while the person completes the form`() {
        val viewModel = SignUpViewModel(FakeSessionRepository(MockRutaUStore()))

        viewModel.onEmailChange("ana")

        assertEquals("Ingresa un correo electrónico válido.", viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onFullNameChange("Ana García")
        viewModel.onUniversityChange("university-id")
        viewModel.onCampusChange("campus-id")
        viewModel.onEmailChange("ana@ejemplo.com")
        viewModel.onPasswordChange("RutaU123")
        viewModel.onConfirmPasswordChange("RutaU123")
        viewModel.onTermsAcceptedChange(true)

        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.confirmPasswordError)
        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `shows a confirmation error as soon as the passwords differ`() {
        val viewModel = SignUpViewModel(FakeSessionRepository(MockRutaUStore()))

        viewModel.onPasswordChange("RutaU123")
        viewModel.onConfirmPasswordChange("OtraClave1")

        assertEquals("Las contraseñas no coinciden.", viewModel.uiState.value.confirmPasswordError)
        assertFalse(viewModel.uiState.value.isFormValid)
    }
}
