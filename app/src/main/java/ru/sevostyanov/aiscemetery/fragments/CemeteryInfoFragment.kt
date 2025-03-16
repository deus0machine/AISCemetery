package ru.sevostyanov.aiscemetery.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import ru.sevostyanov.aiscemetery.R

class CemeteryInfoFragment : Fragment() {
    private val buttons = mutableListOf<Button>()
    private var currentPlayer = "X"
    private val board = Array(9) { "" }
    private lateinit var gameStatus: TextView
    private lateinit var restartButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cemetery_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        gameStatus = view.findViewById(R.id.game_status)
        restartButton = view.findViewById(R.id.btn_restart)

        // Инициализация кнопок
        for (i in 0..8) {
            val buttonId = resources.getIdentifier("btn_$i", "id", requireContext().packageName)
            val button = view.findViewById<Button>(buttonId)
            buttons.add(button)
            
            button.setOnClickListener { 
                handleMove(i)
                animateButton(button)
            }
        }

        restartButton.setOnClickListener { resetGame() }
    }

    private fun handleMove(position: Int) {
        if (board[position].isEmpty()) {
            board[position] = currentPlayer
            buttons[position].text = currentPlayer
            
            if (checkWin()) {
                gameStatus.text = "Игрок $currentPlayer победил!"
                disableButtons()
            } else if (board.none { it.isEmpty() }) {
                gameStatus.text = "Ничья!"
            } else {
                currentPlayer = if (currentPlayer == "X") "O" else "X"
                gameStatus.text = "Ход игрока $currentPlayer"
            }
        }
    }

    private fun checkWin(): Boolean {
        val winPositions = arrayOf(
            arrayOf(0, 1, 2), arrayOf(3, 4, 5), arrayOf(6, 7, 8), // горизонтали
            arrayOf(0, 3, 6), arrayOf(1, 4, 7), arrayOf(2, 5, 8), // вертикали
            arrayOf(0, 4, 8), arrayOf(2, 4, 6) // диагонали
        )

        for (positions in winPositions) {
            if (board[positions[0]] == board[positions[1]] &&
                board[positions[1]] == board[positions[2]] &&
                board[positions[0]].isNotEmpty()
            ) {
                // Анимация победной линии
                for (pos in positions) {
                    animateWinningButton(buttons[pos])
                }
                return true
            }
        }
        return false
    }

    private fun animateButton(button: Button) {
        ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.2f, 1f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.2f, 1f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun animateWinningButton(button: Button) {
        ObjectAnimator.ofFloat(button, "rotation", 0f, 360f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun disableButtons() {
        buttons.forEach { it.isEnabled = false }
    }

    private fun resetGame() {
        board.fill("")
        buttons.forEach { button ->
            button.text = ""
            button.isEnabled = true
            button.rotation = 0f
        }
        currentPlayer = "X"
        gameStatus.text = "Ваш ход!"
    }
} 