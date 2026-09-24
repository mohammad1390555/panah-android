package ir.panahannet.panah

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit val store: Store
    private lateinit val toolbar: MaterialToolbar
    private lateinit val list: RecyclerView
    private lateinit val empty: LinearLayout
    private lateinit val emptyTitle: TextView
    private lateinit val emptyHint: TextView
    private lateinit val fab: FloatingActionButton
    private lateinit val root: View

    private val io = Executors.newSingleThreadExecutor()
    private val subs = mutableListOf<Sub>()
    private val nodes = mutableListOf<Node>()
    private val open: Sub? = null
    private val subAdapter = SubAdapter()
    private val nodeAdapter = NodeAdapter()
    private val fa = Locale("fa", "IR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = Store(this)
        subs.clear()
        subs.addAll(store.all())

        root = findViewById(R.id.root)
        toolbar = findViewById(R.id.toolbar)
        list = findViewById(R.id.list)
        empty = findViewById(R.id.empty)
        emptyTitle = findViewById(R.id.emptyTitle)
        emptyHint = findViewById(R.id.emptyHint)
        fab = findViewById(R.id.fab)

        setSupportActionBar(toolbar)
        list.layoutManager = LinearLayoutManager(this)
        showSubs()

        fab.setOnClickListener {
            if (open == null) addDialog() else refresh(open?)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (open != null) showSubs() else finish()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "به‌روزرسانی همه").setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, 2, 1, "درباره").setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(1)?.isVisible = open == null && subs.isNotEmpty()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId) {
            android.R.id.home -> showSubs()
            1 -> refreshAll()
            2 -> about()
        }
        return true
    }

    private fun showSubs() {
        open = null
        toolbar.title = getString(R.string.app_name)
        toolbar.subtitle = if (subs.isEmpty()) null else "${subs.size} سابسکریپشن"
        toolbar.navigationIcon = null
        fab.setImageResource(R.drawable.ic_add)
        fab.contentDescription = "افزودن"
        list.adapter = subAdapter
        subAdapter.notifyDataSetChanged()
        renderEmpty(
            subs.isEmpty(),
            "هنوز چیزی نیست",
            "سابسکریپشن پنل را با دکمه + اضافه کن."
        )
        invalidateOptionsMenu()
    }

    private fun showNodes(sub: Sub) {
        open = sub
        nodes.clear()
        if (sub.cache.isNotBlank()) nodes.addAll(SubParser.parse(sub.cache))
        toolbar.title = sub.name
        toolbar.subtitle = when {
            sub.lastError.isNotBlank() && nodes.isEmpty() -> sub.lastError
            nodes.isEmpty() -> "کش خالی — برای گرفتن لیست، تازه کن"
            else -> "${nodes.size} کانفیگ"
        }
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { showSubs() }
        fab.setImageResource(R.drawable.ic_refresh)
        fab.contentDescription = "تازه‌سازی"
        list.adapter = nodeAdapter
        nodeAdapter.notifyDataSetChanged()
        renderEmpty(
            nodes.isEmpty(),
            if (sub.lastError.isNotBlank()) "خطا در دریافت" else "کانفیگی نیست",
            sub.lastError.ifBlank { "دکمه تازه‌سازی را بزن." }
        )
        invalidateOptionsMenu()
    }

    private fun renderEmpty(show: Boolean, title: String, hint: String) {
        empty.visibility = if (show) View.VISIBLE else View.GONE
        list.visibility = if (show) View.GONE else View.VISIBLE
        emptyTitle.text = title
        emptyHint.text = hint
    }

    private fun persist() = store.save(subs)

    private fun addDialog(existing: Sub? = null) {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        val name = EditText(this).apply {
            hint = "نام (مثلاً پنل اصلی)"
            setText(existing?.name.orEmpty())
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val url = EditText(this).apply {
            hint = "لینک سابسکریپشن"
            setText(existing?.url.orEmpty())
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        box.addView(name)
        box.addView(url)
        MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "سابسکریپشن جدید" else "ویرایش")
            .setView(box)
            .setPositiveButton("ذخیره") { _, _ ->
                val n = name.text.toString().trim().ifEmpty { "بدون‌نام" }
                val u = url.text.toString().trim()
                if (u.isEmpty()) {
                    toast("لینک خالی است")
                    return@setPositiveButton
                }
                if (existing == null) {
                    val s = Sub(UUID.randomUUID().toString(), n, u)
                    subs.add(0, s)
                    persist()
                    showSubs()
                    refresh(s)
                } else {
                    existing.name = n
                    existing.url = u
                    persist()
                    showSubs()
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun refresh(sub: Sub) {
        Snackbar.make(root, "در حال گرفتن لیست…", Snackbar.LENGTH_SHORT).show()
        io.execute {
            try {
                val body = Fetcher.get(sub.url)
                val parsed = SubParser.parse(body)
                sub.cache = body
                sub.nodeCount = parsed.size
                sub.updatedAt = System.currentTimeMillis()
                sub.lastError = if (parsed.isEmpty()) "فرمت ناشناس یا لیست خالی" else ""
                persist()
                runOnUiThread {
                    if (open?.id == sub.id) showNodes(sub) else showSubs()
                    toast(if (parsed.isEmpty()) "چیزی پیدا نشد" else "${parsed.size} کانفیگ")
                }
            } catch (e: Exception) {
                sub.lastError = e.message ?: "خطا"
                persist()
                runOnUiThread {
                    if (open?.id == sub.id) showNodes(sub) else showSubs()
                    toast(sub.lastError)
                }
            }
        }
    }

    private fun refreshAll() {
        if (subs.isEmpty()) return
        toast("به‌روزرسانی ${subs.size} مورد…")
        subs.toList().forEach { refresh(it) }
    }

    private fun copy(text: String, ok: String = "کپی شد") {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("panah", text))
        toast(ok)
    }

    private fun share(text: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "اشتراک"
            )
        )
    }

    private fun about() {
        MaterialAlertDialogBuilder(this)
            .setTitle("پناه  v${BuildConfig.VERSION_NAME}")
            .setMessage(
                "کلاینت سبک سابسکریپشن برای پنل PANAHANNET.\n" +
                    "بدون تونل، بدون گوگل‌پلی، حجم کم.\n\n" +
                    "لینک ساب را بچسبان → لیست کانفیگ‌ها را ببین → کپی یا اشتراک."
            )
            .setPositiveButton("باشه", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).apply { setGravity(Gravity.CENTER, 0, 0); show() }

    private fun whenAgo(ts: Long): String {
        if (ts == 0L) return "هنوز گرفته نشده"
        val sdf = SimpleDateFormat("d MMM  HH:mm", fa)
        return sdf.format(Date(ts))
    }

    inner class SubAdapter : RecyclerView.Adapter<SubVH>() {
        override fun onCreateViewHolder(p: ViewGroup, v: Int) =
            SubVH(LayoutInflater.from(p.context).inflate(R.layout.item_sub, p, false))

        override fun getItemCount() = subs.size
        override fun onBindViewHolder(h: SubVH, i: Int) {
            val s = subs[i]
            h.title.text = s.name
            h.meta.text = buildString {
                append(if (s.nodeCount > 0) "${s.nodeCount} کانفیگ" else "بدون کش")
                append("  ·  ")
                append(whenAgo(s.updatedAt))
                if (s.lastError.isNotBlank()) append("  ·  ").append(s.lastError)
            }
            h.itemView.setOnClickListener { showNodes(s) }
            h.itemView.setOnLongClickListener {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(s.name)
                    .setItems(arrayOf("ویرایش", "کپی لینک", "حذف")) { _, which ->
                        if (which) {
                            0 -> addDialog(s)
                            1 -> copy(s.url, "لینک کپی شد")
                            2 -> {
                                subs.remove(s)
                                persist()
                                showSubs()
                            }
                        }
                    }
                    .show()
                true
            }
        }
    }

    class SubVH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val meta: TextView = v.findViewById(R.id.meta)
    }

    inner class NodeAdapter : RecyclerView.Adapter<NodeVH>() {
        override fun onCreateViewHolder(p: ViewGroup, v: Int) =
            NodeVH(LayoutInflater.from(p.context).inflate(R.layout.item_node, p, false))

        override fun getItemCount() = nodes.size
        override fun onBindViewHolder(h: NodeVH, i: Int) {
            val n = nodes[i]
            h.badge.text = n.protocol
            h.badge.setBackgroundColor(colorFor(n.protocol))
            h.title.text = n.name
            h.meta.text = n.endpoint()
            h.itemView.setOnClickListener { copy(n.raw, "کانفیگ کپی شد") }
            h.itemView.setOnLongClickListener {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(n.name)
                    .setItems(arrayOf("کپی کانفیگ", "کپی آدرس", "اشتراک")) { _, which ->
                        if (which) {
                            0 -> copy(n.raw)
                            1 -> copy(n.endpoint())
                            2 -> share(n.raw)
                        }
                    }
                    .show()
                true
            }
        }
    }

    class NodeVH(v: View) : RecyclerView.ViewHolder(v) {
        val badge: TextView = v.findViewById(R.id.badge)
        val title: TextView = v.findViewById(R.id.title)
        val meta: TextView = v.findViewById(R.id.meta)
    }

    private fun colorFor(p: String): Int {
        val id = if (p) {
            "VLESS" -> 0xFF2563EB.toInt()
            "VMess" -> 0xFF7C3AED.toInt()
            "Trojan" -> 0xFFDC2626.toInt()
            "SS" -> 0xFFD97706.toInt()
            "HY2", "HY" -> 0xFF0891B2.toInt()
            "TUIC" -> 0xFFDB2777.toInt()
            "WG" -> 0xFF059669.toInt()
            else -> 0xFF475569.toInt()
        }
        return id
    }
}
