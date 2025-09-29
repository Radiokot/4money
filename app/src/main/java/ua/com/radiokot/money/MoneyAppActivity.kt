package ua.com.radiokot.money

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.money.auth.data.UserSession
import ua.com.radiokot.money.auth.logic.createActivityScopeWithSession
import ua.com.radiokot.money.auth.view.AuthActivity
import ua.com.radiokot.money.lock.logic.AppLock
import ua.com.radiokot.money.lock.view.UnlockActivity

abstract class MoneyAppActivity(
    protected open val requiresUnlocking: Boolean,
    protected open val requiresSession: Boolean,
) :
    AppCompatActivity(),
    AndroidScopeComponent {

    override val scope: Scope = createActivityScopeWithSession()
    private val log by lazyLogger("MoneyAppActivity")

    protected val lock: AppLock by inject()
    protected val unlockLauncher = registerForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        callback = { result ->
            isUnlockLaunched = false
            if (result.resultCode != RESULT_OK) {
                finishAffinity()
            }
        },
    )
    private var isUnlockLaunched = false

    protected open val hasSession: Boolean
        get() = scope.getOrNull<UserSession>() != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requiresSession && goToAuthIfNoSession()) {
            return
        }

        unlockAppIfNeeded()

        onCreateAllowed(savedInstanceState)
    }

    abstract fun onCreateAllowed(savedInstanceState: Bundle?)

    override fun onResume() {
        super.onResume()
        unlockAppIfNeeded()
    }

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

    protected open fun unlockAppIfNeeded() {
        if (requiresUnlocking && lock.isLocked && !isUnlockLaunched) {

            log.debug {
                "unlockAppIfNeeded(): launching the unlock screen"
            }

            isUnlockLaunched = true

            unlockLauncher.launch(Intent(this, UnlockActivity::class.java))
        }
    }

    /**
     * Goes to the [ua.com.radiokot.money.auth.view.AuthActivity] finishing this activity and all the underlying.
     */
    protected open fun goToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finishAffinity()
    }
}
