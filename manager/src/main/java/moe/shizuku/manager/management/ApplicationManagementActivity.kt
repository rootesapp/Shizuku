package moe.shizuku.manager.management

import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.AppsActivityBinding
import moe.shizuku.manager.utils.CustomTabsHelper
import rikka.lifecycle.Status
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.Shizuku
import java.util.*

class ApplicationManagementFragment : Fragment() {

    private var _binding: AppsActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<AppsViewModel>() // 注意这里可能需要调整 ViewModel 初始化方式
    private val adapter = AppsAdapter()

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        if (!isRemoving) {
            requireActivity().finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        
        // 检查 Shizuku 服务状态
        if (!Shizuku.pingBinder()) {
            requireActivity().finish()
            return
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AppsActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置返回按钮
        (requireActivity() as AppBarActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 观察数据变化
        viewModel.packages.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    adapter.updateData(it.data)
                }
                Status.ERROR -> {
                    requireActivity().finish()
                    val tr = it.error
                    Toast.makeText(requireContext(), Objects.toString(tr, "unknown"), Toast.LENGTH_SHORT).show()
                    tr.printStackTrace()
                }
                Status.LOADING -> {
                    
                }
            }
        }

        // 初始化加载数据
        if (viewModel.packages.value == null) {
            viewModel.load()
        }

        // 配置 RecyclerView
        val recyclerView = binding.list
        recyclerView.adapter = adapter
        recyclerView.fixEdgeEffect()
        recyclerView.addEdgeSpacing(top = 8f, bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)

        // 注册数据变化监听
        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                viewModel.loadCount()
            }
        })

        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Shizuku.removeBinderDeadListener(binderDeadListener)
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.management, menu) // 确保有对应的菜单资源
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            R.id.action_help -> {
                CustomTabsHelper.open(requireContext(), Helps.APPLICATION_MANAGEMENT.get())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}

//rootes by
