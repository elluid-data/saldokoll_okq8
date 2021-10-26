package se.adanware.visaparser.fragment

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import se.adanware.visaparser.databinding.FragmentLoginViewBinding
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import se.adanware.visaparser.R
import se.adanware.visaparser.data.observeEvent
import se.adanware.visaparser.model.BankViewModel
import se.adanware.visaparser.model.LoginStatus
import se.adanware.visaparser.model.UserViewModel
import se.adanware.visaparser.repository.PreferencesRepository

const val PROGRESS_DIALOG_TAG = "progressDialog"

class LoginFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels()
    private val bankViewModel: BankViewModel by activityViewModels()


    private lateinit var binding: FragmentLoginViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLoginViewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        val prefs = PreferencesRepository(requireContext())
        userViewModel.username.value = prefs.getSsn()
        userViewModel.setSaveCredentials(prefs.isSavingCredentials())
        binding.viewModel = userViewModel
        binding.loginFragment = this
        setHasOptionsMenu(true)
        AppCompatDelegate.setDefaultNightMode(PreferencesRepository(requireContext()).getNightMode())
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_toggle_theme -> {
                val isNightTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                when (isNightTheme) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        PreferencesRepository(requireContext()).setNightMode(1)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    Configuration.UI_MODE_NIGHT_NO -> {
                        PreferencesRepository(requireContext()).setNightMode(2)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }

                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ssnEdittext.setOnKeyListener { view, keyCode, _ -> handleKeyEvent(view, keyCode)  }
        bankViewModel.signInStatus.observeEvent(viewLifecycleOwner) {
            // Update progress
            when(it) {
                LoginStatus.AWAITING_BANKID -> updateProgressDialog(getString(R.string.signin))
                LoginStatus.CANCELLED -> {
                    dismissProgressDialog()
                    Toast.makeText(context, getString(R.string.bankid_cancelled), Toast.LENGTH_LONG).show()
                }
                LoginStatus.NOT_SIGNED -> showBankIdRetryDialog()
                LoginStatus.GET_SESSION -> updateProgressDialog(getString(R.string.retrieve_data))
                LoginStatus.GET_SESSION_FAILED -> {
                    dismissProgressDialog()
                    createErrorMessageDialog(bankViewModel.errorMessage)
                }
                LoginStatus.COMPLETE -> {
                    dismissProgressDialog()
                    findNavController().navigate(R.id.action_loginFragment_to_accountFragment)

                }
            }
        }

        bankViewModel.signInErrors.observe(viewLifecycleOwner) {
                errorList ->
            for (error in errorList) {
                createErrorMessageDialog(error.second)
            }
        }
    }

    fun validateUsername() {
        hideKeyboard()
        if(userViewModel.validateUsername()) {
            val prefs = PreferencesRepository(requireContext())
            if(userViewModel.saveCredentials.value == true) {
                prefs.saveSsn(userViewModel.username.value!!)
            }
            connect(userViewModel.username.value!!)
        }
    }


    private fun handleKeyEvent(view: View, keyCode: Int) : Boolean{
        if(keyCode == KeyEvent.KEYCODE_ENTER) {
            binding.ssnEdittext.clearFocus()
            hideKeyboard()
            return true
        }
        return false
    }

    private fun hideKeyboard() {
        val inputManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun connect(username: String) {
        // Break if BankID not found.
        if(!isBankIdAvailable(requireContext())) {
            Toast.makeText(context, getString(R.string.bankid_missing), Toast.LENGTH_LONG).show()
            return
        }
        showProgressDialog()
        bankViewModel.startSignIn(username)
    }

    private fun showBankIdRetryDialog() {
        AlertDialog.Builder(requireContext()).also {
            it.setMessage(getString(R.string.bankid_not_done))
            it.setCancelable(true)
            it.setNegativeButton("Avbryt") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            it.setPositiveButton(getString(R.string.anslut_igen)) { dialogInterface, _ ->
                dialogInterface.dismiss()
                bankViewModel.checkSignInStatus()
            }
        }.create().show()
    }

    fun toggleCredentials(isChecked: Boolean) {
        userViewModel.setSaveCredentials(isChecked)
        val prefs = PreferencesRepository(requireContext())
        prefs.setSaveCredentials(isChecked)
    }

    private fun isBankIdAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage("com.bankid.bus") ?: return false
        val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.size > 0
    }

    private fun createErrorMessageDialog(message: String?) {

        AlertDialog.Builder(requireContext()).also {
            it.setTitle(getString(R.string.error_signin))
            it.setMessage(message)
            it.setCancelable(true)
            it.setPositiveButton(getString(R.string.ok)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
        }.create().show()
    }

    private fun showProgressDialog() {
        var fragment : StatusDialog? = parentFragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG) as StatusDialog?
        if(fragment == null) {
            fragment = StatusDialog()
        }
        fragment.show(parentFragmentManager, PROGRESS_DIALOG_TAG)
    }

    private fun updateProgressDialog(text: String) {
        val fragment = parentFragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG) as StatusDialog?
        fragment?.updateProgress(text)
    }

    private fun dismissProgressDialog() {
        val fragment = parentFragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG) as StatusDialog?
        fragment?.dismiss()
    }

}