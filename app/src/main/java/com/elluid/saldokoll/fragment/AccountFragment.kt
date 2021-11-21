package com.elluid.saldokoll.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.elluid.saldokoll.adapter.ItemAdapter
import com.elluid.saldokoll.databinding.FragmentAccountBinding
import com.elluid.saldokoll.okq8.OKQ8Card
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import java.io.File
import android.view.*
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.elluid.saldokoll.R
import com.elluid.saldokoll.data.observeEvent
import com.elluid.saldokoll.model.BankViewModel
import com.elluid.saldokoll.model.DownloadStatus
import com.elluid.saldokoll.model.PDF_DIRECTORY
import com.elluid.saldokoll.model.PDF_FILENAME


class AccountFragment : Fragment() {
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private var rotationAngle = 0f
    lateinit var okq8Card: OKQ8Card
    lateinit var animator: ObjectAnimator

    private val bankViewModel: BankViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        bankViewModel.downloadEvent.observeEvent(viewLifecycleOwner) {
                status ->
            when(status) {
                DownloadStatus.INCOMPLETE -> Toast.makeText(context, getString(R.string.pdf_not_done), Toast.LENGTH_SHORT).show()
                DownloadStatus.ERROR -> {
                    Toast.makeText(context, getString(R.string.pdf_error), Toast.LENGTH_LONG).show()
                }
                DownloadStatus.COMPLETE -> startPdfIntent()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if(bankViewModel.isOkq8ServiceAlive()) {
            okq8Card = bankViewModel.okQ8Service.getCard()
        } else {
            logOut()
            return
        }

        initDetailsCardView()
        initListView()
        initCopyOcr()
        animator = ObjectAnimator.ofFloat(binding.detailsView.btnExpand, "rotation", rotationAngle, rotationAngle+180).also {
            it.duration = 700
        }
        binding.detailsView.btnExpand.setOnClickListener {
            toggleExpandedView()
        }
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.account_menu, menu);
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_logout -> {
               logOut()
               true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initDetailsCardView() {
        binding.detailsView.textviewCreditsTotal.setText(okq8Card.getLimit())
        binding.detailsView.tvCreditsLeft.setText(okq8Card.getBalance())
        binding.detailsView.tvCreditsUsedSum.setText(okq8Card.getAvailableBalance())
        binding.detailsView.tvOcrData.setText(okq8Card.ocr)
        binding.detailsView.tvOwnerData.setText(okq8Card.name)
        binding.detailsView.tvDueDateData.setText(okq8Card.invoice.dueDate)
        binding.detailsView.tvInvoiceAmountData.setText(okq8Card.invoice.totalAmount)
        if(okq8Card.invoice.pdfUrl.isEmpty()) {
            binding.detailsView.buttonPdf.isEnabled = false
        } else {
            binding.detailsView.buttonPdf.setOnClickListener {
                bankViewModel.retrieveInvoice(requireContext().filesDir)
            }
        }
    }

    private fun initListView() {
        val recyclerView = binding.transactionsView.listviewTransactions
        recyclerView.adapter = ItemAdapter(requireContext(), okq8Card.transactions)
        val dividerItemDecoration = DividerItemDecoration(context, VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)
    }

    private fun initCopyOcr() {
        binding.detailsView.copyToClipboard.setOnClickListener {
            val ocr = binding.detailsView.tvOcrData.text.toString()
            val clipboard: ClipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OCR", ocr)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context,"Kopierade $ocr till urklipp.", Toast.LENGTH_LONG).show()
        }
    }
    private fun toggleExpandedView() {

        animateExpandButton()
        if(binding.detailsView.expandedViewGroup.isVisible) {
            binding.detailsView.expandedViewGroup.visibility = View.GONE
        }
        else {
            binding.detailsView.expandedViewGroup.visibility = View.VISIBLE
        }
    }

    private fun animateExpandButton() {
        animator.start()
        animator.setFloatValues(rotationAngle, rotationAngle + 180)
        rotationAngle += 180
        rotationAngle = if(rotationAngle == 360f) 0f else 180f
    }

    private fun startPdfIntent() {
        val intent = Intent(Intent.ACTION_VIEW)
        val fileDir = File(context?.filesDir, PDF_DIRECTORY)
        val file = File(fileDir, PDF_FILENAME)
        val uri = FileProvider.getUriForFile(requireContext(), "se.adanware.fileprovider", file)
        intent.setDataAndType(uri, "application/pdf")
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // Check if there's any package that can open up the file.
        if(activity?.packageManager?.resolveActivity(intent, 0) != null) {
            startActivity(intent)
        }
    }

    private fun logOut() {
        findNavController().navigate(R.id.action_accountFragment_to_loginFragment)
    }
}