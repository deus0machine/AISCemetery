package ru.sevostyanov.aiscemetery.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.fragments.TaskFragment
import ru.sevostyanov.aiscemetery.memorial.BurialFormFragment

class TaskChoiceFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_choice, container, false)

        val btnGraveExists = view.findViewById<Button>(R.id.btn_grave_exists)
        val btnNoGrave = view.findViewById<Button>(R.id.btn_no_grave)

        btnGraveExists.setOnClickListener {
            // Переход к списку услуг
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, TaskFragment())
                .addToBackStack(null)
                .commit()
        }

        btnNoGrave.setOnClickListener {
            // Переход на фрагмент с формой создания захоронения
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, BurialFormFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}