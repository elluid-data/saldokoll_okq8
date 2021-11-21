package com.elluid.saldokoll.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.elluid.saldokoll.R
import com.elluid.saldokoll.okq8.Transaction

class ItemAdapter(private val context: Context, private val dataset: List<Transaction>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val merchantTextView: TextView = view.findViewById(R.id.textview_merchant)
        val amountTextView: TextView = view.findViewById(R.id.textview_amount)
        val reservedTextView: TextView = view.findViewById(R.id.textview_reserved)
        val dateTextView: TextView = view.findViewById(R.id.textview_date)
        val foreignAmountTextView: TextView = view.findViewById(R.id.textview_foreign_amount)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.amountTextView.setText(item.getAmount())
        if(item.merchantCity.isNotEmpty()) {
            holder.merchantTextView.text = "${item.merchantName}, ${item.merchantCity}"
        }
        else {
            holder.merchantTextView.text = item.merchantName
        }

        holder.dateTextView.text = item.getDate()
        if(item.isReserved) {
            holder.merchantTextView.setTypeface(holder.merchantTextView.typeface, Typeface.ITALIC)
            holder.reservedTextView.setTypeface(holder.reservedTextView.typeface, Typeface.ITALIC)
            holder.reservedTextView.visibility = View.VISIBLE
            holder.reservedTextView.text = context.resources.getString(R.string.reserved_amount)
        }
        if(item.isPayment) {
            holder.merchantTextView.setTextColor(ContextCompat.getColor(context, R.color.green_500))
            holder.amountTextView.setTextColor(ContextCompat.getColor(context, R.color.green_500))
        }

        if(item.isForeignCurrency) {
            holder.foreignAmountTextView.visibility = View.VISIBLE
            holder.foreignAmountTextView.text = context.getString(R.string.foreign_text, item.getForeignAmount())
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount() = dataset.size

}