package com.example.bankapputomo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bankapputomo.util.BranchModel
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class BranchAdapter(
    private var items: List<BranchModel>,
    private val onItemClick: (BranchModel) -> Unit
) : RecyclerView.Adapter<BranchAdapter.BranchViewHolder>() {

    private var selectedBranchId: String? = null

    fun updateData(newItems: List<BranchModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelectedId(branchId: String?) {
        selectedBranchId = branchId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_branch_figma, parent, false)
        return BranchViewHolder(view)
    }

    override fun onBindViewHolder(holder: BranchViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedBranchId)
    }

    override fun getItemCount(): Int = items.size

    inner class BranchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textBranchName)
        private val textAddress: TextView = itemView.findViewById(R.id.textBranchAddress)
        private val textHours: TextView = itemView.findViewById(R.id.textBranchHours)
        private val textDistance: TextView = itemView.findViewById(R.id.textBranchDistance)

        fun bind(branch: BranchModel, isSelected: Boolean) {
            textName.text = branch.name
            textAddress.text = branch.address
            textHours.text = if (branch.hours.isNotEmpty()) branch.hours else branch.services
            textDistance.text = branch.distance

            if (isSelected) {
                itemView.setBackgroundColor(0x183B49DF)
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
            }

            itemView.bounceClick {
                onItemClick(branch)
            }
        }
    }
}
