package kz.abdiyev.walletai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ui.screens.TransactionDialog
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDialogUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"),
        CategoryData(2, "Зарплата", TransactionType.INCOME, "#9CCC65", "payments")
    )

    @Test
    fun dialog_showsTitleAndCategoryChips() {
        composeTestRule.setContent {
            WalletAiTheme {
                TransactionDialog(
                    categories = categories,
                    currency = Currency.KZT,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithText("Новая операция").assertIsDisplayed()
        composeTestRule.onNodeWithText("Продукты").assertIsDisplayed()
    }

    @Test
    fun confirmButton_isClickable() {
        var confirmed = false
        composeTestRule.setContent {
            WalletAiTheme {
                TransactionDialog(
                    categories = categories,
                    currency = Currency.KZT,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _ -> confirmed = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Добавить").performClick()
        // Without amount/description confirm stays false — button must remain interactive
        composeTestRule.onNodeWithText("Добавить").assertIsDisplayed()
        assert(!confirmed)
    }
}
