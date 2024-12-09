package ru.sevostyanov.aiscemetery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class BurialsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Подключаем макет для фрагмента
        return inflater.inflate(R.layout.fragment_burials, container, false)
    }
}