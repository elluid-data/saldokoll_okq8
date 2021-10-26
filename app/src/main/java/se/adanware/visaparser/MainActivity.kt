package se.adanware.visaparser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import se.adanware.visaparser.model.BankViewModel
import se.adanware.visaparser.model.LoginStatus
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration


class MainActivity : AppCompatActivity() {

    private val bankViewModel: BankViewModel by viewModels()
    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bankViewModel.signInStatus.observe(this) {
            loginStatus ->
                if(loginStatus == LoginStatus.BANKID_RECIEVED) {
                    startBankIDSigning()
                }
        }
        setupActionBarWithNavController(navController, AppBarConfiguration.Builder(R.id.loginFragment, R.id.accountFragment).build())
    }

    fun startBankIDSigning() {
        val intent = Intent().also {
            it.setPackage("com.bankid.bus")
            it.action = Intent.ACTION_VIEW
            it.type = "bankid"
            it.data = Uri.parse("bankid://www.bankid.com?redirect=null")
        }

        startForResult.launch(intent)
    }

    val startForResult = registerForActivityResult(StartActivityForResult()) { _: ActivityResult ->
        // BankID never sends any data back so just check when activity comes back into foreground.
        bankViewModel.checkSignInStatus()
    }

}


