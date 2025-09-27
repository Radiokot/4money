package ua.com.radiokot.money

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.money.auth.data.UserSession
import ua.com.radiokot.money.auth.logic.createActivityScopeWithSession
import ua.com.radiokot.money.auth.view.AuthActivity
import ua.com.radiokot.money.lock.logic.AppLock

abstract class MoneyAppActivity :
    AppCompatActivity(),
    AndroidScopeComponent {

    override val scope: Scope = createActivityScopeWithSession()
    private val log by lazyLogger("MoneyAppActivity")

    protected val lock: AppLock by inject()
    protected open val hasSession: Boolean
        get() = scope.getOrNull<UserSession>() != null

    /**
     * @return true if there is no session and switching to the auth screen has been called.
     */
    open fun goToAuthIfNoSession(): Boolean =
        if (!hasSession) {
            log.debug {
                "goToAuthIfNoSession(): going"
            }
            goToAuth()
            true
        } else {
            false
        }

    /**
     * @return true if there is no session and finishing has been called.
     */
    open fun finishIfNoSession(): Boolean =
        if (!hasSession) {
            log.debug {
                "finishIfNoSession(): finishing"
            }
            finishAffinity()
            true
        } else {
            false
        }

    /**
     * Goes to the [ua.com.radiokot.money.auth.view.AuthActivity] finishing this activity and all the underlying.
     */
    protected open fun goToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finishAffinity()
    }
}
