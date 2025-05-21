package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.ui.genealogy.GenealogyTreeScreen
import ru.sevostyanov.aiscemetery.viewmodels.FamilyTreeDetailViewModel

@AndroidEntryPoint
class GenealogyTreeFragment : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(treeId: Long): GenealogyTreeFragment {
            val fragment = GenealogyTreeFragment()
            val args = Bundle()
            args.putLong("treeId", treeId)
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel: FamilyTreeDetailViewModel by viewModels()
    private val treeId: Long by lazy { arguments?.getLong("treeId") ?: -1L }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.loadGenealogyData(treeId)
        return ComposeView(requireContext()).apply {
            setContent {
                val tree by viewModel.familyTree.observeAsState()
                val relations by viewModel.memorialRelations.observeAsState(emptyList())
                val memorials = relations.flatMap { listOf(it.sourceMemorial, it.targetMemorial) }.distinctBy { it.id }
                GenealogyTreeScreen(
                    memorials = memorials,
                    relations = relations
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
} 