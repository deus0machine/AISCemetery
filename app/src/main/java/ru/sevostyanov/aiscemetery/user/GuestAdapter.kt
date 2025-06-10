package ru.sevostyanov.aiscemetery.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R

class GuestAdapter(
    private val guests: MutableList<GuestItem>,
    private val onDeleteClick: (GuestItem) -> Unit
) : RecyclerView.Adapter<GuestAdapter.GuestViewHolder>() {

    class GuestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.guest_name)
        val contacts: TextView = view.findViewById(R.id.guest_contacts)
        val dateOfRegistration: TextView = view.findViewById(R.id.guest_date_of_registration)
        val login: TextView = view.findViewById(R.id.guest_login)
        val balance: TextView = view.findViewById(R.id.guest_balance)
        val deleteButton: Button = view.findViewById(R.id.btn_delete_guest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_guest, parent, false)
        return GuestViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuestViewHolder, position: Int) {
        val guest = guests[position]
        holder.name.text = guest.fio
        holder.contacts.text = guest.contacts ?: "No Contacts"
        holder.dateOfRegistration.text = "Registered: ${guest.dateOfRegistration}"
        holder.login.text = "Login: ${guest.login}"
        //holder.balance.text = "Balance: ${guest.balance} ₽"
        holder.deleteButton.setOnClickListener { onDeleteClick(guest) }
    }

    override fun getItemCount(): Int = guests.size

    fun removeGuest(guestId: Long) {
        val index = guests.indexOfFirst { it.id == guestId }
        if (index != -1) {
            guests.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}


