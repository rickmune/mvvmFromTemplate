package app.web.drjackycv.presentation.products.productlist

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import app.web.drjackycv.domain.base.Failure
import app.web.drjackycv.domain.base.RecyclerItem
import app.web.drjackycv.domain.products.entity.Beer
import app.web.drjackycv.presentation.R
import app.web.drjackycv.presentation.base.adapter.LoadingStateAdapter
import app.web.drjackycv.presentation.base.compose.*
import app.web.drjackycv.presentation.databinding.FragmentProductListBinding
import app.web.drjackycv.presentation.extension.*
import com.google.android.material.transition.platform.Hold
import com.google.android.material.transition.platform.MaterialElevationScale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsListFragment : Fragment(R.layout.fragment_product_list) {

    val binding by viewBinding(FragmentProductListBinding::bind)

    private val productsListViewModel: ProductsListViewModel by viewModels()

    private val productsListAdapter: ProductsListAdapter by lazy {
        ProductsListAdapter(::navigateToProductDetail)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val composeView = ComposeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setContent {
                ProductsListComposable {
                    MyScreenContent()
                }
            }
        }
        setupRecycler()
        setupViewModel()
        binding.parentContainer.addView(composeView)
    }

    private fun setupRecycler() {
        binding.inclItemError.itemErrorContainer.gone()
        binding.productListRecyclerView.adapter =
            productsListAdapter.withLoadStateFooter(LoadingStateAdapter())
        productsListAdapter.addLoadStateListener { adapterLoadingErrorHandling(it) }
        productsListAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY

        //productListRecyclerView.itemAnimator = null
        postponeEnterTransition()
        view?.doOnPreDraw { startPostponedEnterTransition() }
    }

    private fun setupViewModel() {
        productsListViewModel.run {

            observe(ldProductsList, ::addProductsList)

            observe(ldFailure, ::handleFailure)

        }
    }

    private fun addProductsList(productsList: PagingData<RecyclerItem>) {
        binding.productListRecyclerView.visible()
        productsListAdapter.submitData(lifecycle, productsList)
    }

    private fun loadingUI(isLoading: Boolean) {
        binding.inclItemError.itemErrorContainer.gone()
        if (isLoading) {
            binding.inclItemLoading.itemLoadingContainer.visible()
        } else {
            binding.inclItemLoading.itemLoadingContainer.gone()
        }
    }

    private fun handleFailure(failure: Failure) {
        when (failure) {
            is Failure.FailureWithMessage -> {
                binding.productListRecyclerView.gone()
                binding.inclItemError.itemErrorContainer.visible()
                binding.inclItemError.itemErrorMessage.text = failure.msg
                binding.inclItemError.itemErrorRetryBtn.setOnClickListener { failure.retryAction() }
            }
            else -> {
                binding.productListRecyclerView.gone()
                binding.inclItemError.itemErrorContainer.visible()
                binding.inclItemError.itemErrorMessage.text = failure.message
                binding.inclItemError.itemErrorRetryBtn.invisible()
            }
        }
    }

    private fun navigateToProductDetail(item: RecyclerItem, view: View) {
        val itemUI = BeerMapper().mapToUI(item as Beer)
        val action =
            ProductsListFragmentDirections.navigateToProductDetailFragment(itemUI)
        val extras = FragmentNavigatorExtras(
            view to itemUI.id.toString()
        )
        exitTransition = Hold().apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.motion_duration_small).toLong()
        }
        findNavController().navigate(action, extras)
    }

    private fun adapterLoadingErrorHandling(combinedLoadStates: CombinedLoadStates) {
        if (combinedLoadStates.refresh is LoadState.Loading) {
            loadingUI(true)
        } else {
            loadingUI(false)
            val error = when {
                combinedLoadStates.prepend is LoadState.Error -> combinedLoadStates.prepend as LoadState.Error
                combinedLoadStates.source.prepend is LoadState.Error -> combinedLoadStates.prepend as LoadState.Error
                combinedLoadStates.append is LoadState.Error -> combinedLoadStates.append as LoadState.Error
                combinedLoadStates.source.append is LoadState.Error -> combinedLoadStates.append as LoadState.Error
                combinedLoadStates.refresh is LoadState.Error -> combinedLoadStates.refresh as LoadState.Error
                else -> null
            }
            error?.run {
                productsListViewModel.handleFailure(this.error) { retryFetchData() }
            }
        }
    }

    private fun retryFetchData() {
        productsListViewModel.getProducts("")
    }

}

@Composable
fun ProductsListComposable(content: @Composable () -> Unit) {
    BaseTheme {
        Surface(color = transparent) {
            content()
        }
    }
}

@Composable
fun MyScreenContent() {
    val counterState = remember { mutableStateOf(1) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Greeting(
            name = "Android",
            count = counterState.value,
            updateCount = { newCount ->
                counterState.value = newCount
            }
        )
    }
}

@Composable
fun Greeting(name: String, count: Int, updateCount: (Int) -> Unit) {
    Button(
        onClick = { updateCount(count + 1) },
        colors = ButtonConstants.defaultTextButtonColors(
            backgroundColor = if (count > 7) teal200 else purple200
        )
    ) {
        Text(
            text = "Hello $name $count!",
            modifier = Modifier.padding(4.dp),
            style = TextStyle(fontSize = TextUnit.Sp(11))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProductsListComposable {
        MyScreenContent()
    }
}