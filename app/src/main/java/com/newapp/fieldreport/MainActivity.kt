@file:OptIn(ExperimentalMaterial3Api::class)

package com.newapp.fieldreport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val roles = listOf(
    "مدير المشروع", "مدير الموقع", "المخازن", "مهندس الكهرباء", "مهندس الميكانيك",
    "الجودة", "السلامة", "الحسابات والمالية", "المشتريات"
)

private val commonTitles = listOf(
    "مدير مشروع", "مدير موقع", "مهندس موقع", "مهندس كهرباء", "مهندس ميكانيك",
    "أمين مخزن", "مسؤول مشتريات", "مهندس جودة", "مسؤول سلامة", "محاسب"
)

private val priorities = listOf("Low" to "منخفضة", "Medium" to "متوسطة", "High" to "عالية")
private val statuses = listOf("Open" to "مفتوح", "In Progress" to "قيد المعالجة", "Closed" to "مغلق")

private fun typeFor(r: String) = when (r) {
    "المخازن", "المشتريات" -> "Material"
    "الجودة", "السلامة" -> "Issue"
    "مدير المشروع", "الحسابات والمالية" -> "Progress"
    else -> "Quantity"
}

private fun typeLabel(t: String) = when (t) {
    "Material" -> "تسجيل حركة المواد"
    "Issue" -> "تسجيل البلاغات والملاحظات"
    "Progress" -> "تسجيل المتابعة ونسب الإنجاز"
    else -> "تسجيل الكميات المنفذة"
}

/** Items shown for a role. Falls back to all items so nobody is ever blocked from saving. */
private fun itemsFor(role: String, all: List<Item>): List<Item> {
    val keys: Set<String> = when (role) {
        "مهندس الكهرباء" -> setOf(role, "الكهرباء")
        "مهندس الميكانيك" -> setOf(role, "الميكانيك")
        "الجودة", "السلامة" -> setOf(role, "الجودة والسلامة")
        else -> setOf(role)
    }
    val m = all.filter { it.category in keys }
    return if (m.isEmpty()) all else m
}

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme { App() }
            }
        }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val msg by vm.message.collectAsState()
    val settings by vm.settings.collectAsState()
    val allEntries by vm.entries.collectAsState()
    val lastExport by vm.lastExport.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        u?.let { vm.importFile(ctx.contentResolver, it) }
    }
    LaunchedEffect(msg) {
        if (msg.isNotBlank()) {
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            vm.clearMessage()
        }
    }

    val needSetup = settings.userName.isBlank() || settings.userTitle.isBlank() || settings.projectCode.isBlank()
    val pending = allEntries.count { it.eventAt > lastExport }

    val doExport: (String?) -> Unit = { day ->
        scope.launch {
            try { vm.export(day)?.let { share(ctx, it) } }
            catch (e: Exception) { Toast.makeText(ctx, "تعذر التصدير: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    // Phone back button: settings -> previous screen, role screen -> role list (instead of closing the app).
    BackHandler(enabled = !needSetup && (showSettings || role != null)) {
        if (showSettings) showSettings = false else role = null
    }

    if (showExport) AlertDialog(
        onDismissRequest = { showExport = false },
        title = { Text("تصدير ملف Excel") },
        text = { Text("«تقرير اليوم» يصدّر سجلات تاريخ اليوم فقط. «كل السجلات» يصدّر كل ما في هذا الجهاز.") },
        confirmButton = { TextButton({ showExport = false; doExport(LocalDate.now().toString()) }) { Text("تقرير اليوم") } },
        dismissButton = {
            Row {
                TextButton({ showExport = false; doExport(null) }) { Text("كل السجلات") }
                TextButton({ showExport = false }) { Text("إلغاء") }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(role ?: "Field Report", fontWeight = FontWeight.Bold) },
                navigationIcon = { if (role != null && !showSettings) TextButton({ role = null }) { Text("الأدوار") } }
            )
        },
        bottomBar = {
            if (!needSetup) NavigationBar {
                NavigationBarItem(selected = false, onClick = { picker.launch(arrayOf("*/*")) }, icon = {}, label = { Text("استيراد") })
                NavigationBarItem(selected = false, onClick = { showExport = true }, icon = {}, label = { Text("تصدير") })
                NavigationBarItem(selected = showSettings, onClick = { showSettings = !showSettings }, icon = {}, label = { Text("الإعدادات") })
            }
        }
    ) { p ->
        val mod = Modifier.padding(p)
        when {
            needSetup || showSettings -> SettingsScreen(settings, needSetup, mod) {
                vm.saveSettings(it); showSettings = false
            }
            role == null -> RolePicker(settings, mod, pending, lastExport) { role = it }
            else -> EntryScreen(role!!, vm, mod)
        }
    }
}

@Composable
fun SettingsScreen(s: Settings, firstRun: Boolean, mod: Modifier, onSave: (Settings) -> Unit) {
    var userName by rememberSaveable { mutableStateOf(s.userName) }
    var userTitle by rememberSaveable { mutableStateOf(s.userTitle) }
    var company by rememberSaveable { mutableStateOf(s.company) }
    var orgCode by rememberSaveable { mutableStateOf(s.orgCode) }
    var projectCode by rememberSaveable { mutableStateOf(s.projectCode) }
    var projectName by rememberSaveable { mutableStateOf(s.projectName) }
    var projectNameAr by rememberSaveable { mutableStateOf(s.projectNameAr) }
    var titleMenu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty() }
    LazyColumn(mod.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(if (firstRun) "الإعداد الأولي" else "الإعدادات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("تُدخل هذه البيانات مرة واحدة، وتظهر في كل سجل وفي التقارير.")
        }
        item { Text("بياناتك", fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(userName, { userName = it }, label = { Text("الاسم الكامل") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(userTitle, { userTitle = it }, label = { Text("الصفة الوظيفية") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item {
            Box {
                TextButton({ titleMenu = true }) { Text("اختر صفة شائعة") }
                DropdownMenu(titleMenu, { titleMenu = false }) {
                    commonTitles.forEach { t -> DropdownMenuItem({ Text(t) }, { userTitle = t; titleMenu = false }) }
                }
            }
        }
        item { HorizontalDivider(); Text("الشركة والمشروع", fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(company, { company = it }, label = { Text("اسم الشركة") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(orgCode, { orgCode = it.uppercase().take(6) }, label = { Text("رمز الشركة (مثال: ABC)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(projectCode, { projectCode = it.uppercase().trim() }, label = { Text("رمز المشروع (مثال: VL01)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(projectName, { projectName = it }, label = { Text("اسم المشروع بالإنجليزية") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(projectNameAr, { projectNameAr = it }, label = { Text("اسم المشروع بالعربية") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item {
            Button({
                if (userName.isBlank() || userTitle.isBlank()) Toast.makeText(ctx, "اكتب اسمك وصفتك الوظيفية", Toast.LENGTH_SHORT).show()
                else if (projectCode.isBlank()) Toast.makeText(ctx, "اكتب رمز المشروع", Toast.LENGTH_SHORT).show()
                else onSave(Settings(userName.trim(), userTitle.trim(), company.trim(), orgCode.trim(), projectCode.trim(), projectName.trim(), projectNameAr.trim()))
            }, Modifier.fillMaxWidth()) { Text("حفظ") }
        }
        item { Text("الإصدار: $ver", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun RolePicker(s: Settings, mod: Modifier, pending: Int, lastExport: String, on: (String) -> Unit) {
    LazyColumn(mod.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Image(painterResource(com.newapp.fieldreport.R.drawable.fieldreport_logo), "شعار Field Report", Modifier.size(86.dp), contentScale = ContentScale.Fit)
            Text("Field Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${s.userName} — ${s.userTitle}")
            Text("المشروع: ${s.projectCode}", style = MaterialTheme.typography.bodySmall)
            if (pending > 0) {
                Text(
                    "سجلات لم تُصدَّر بعد: $pending" + (if (lastExport.isNotBlank()) " — آخر تصدير: $lastExport" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        items(roles) { r ->
            Card(onClick = { on(r) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(r, fontWeight = FontWeight.Bold)
                    Text(typeLabel(typeFor(r)), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun Choice(label: String, value: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ open = true }, Modifier.fillMaxWidth()) { Text("$label: ${options.firstOrNull { it.first == value }?.second ?: "—"}") }
        DropdownMenu(open, { open = false }) {
            options.forEach { o -> DropdownMenuItem({ Text(o.second) }, { onPick(o.first); open = false }) }
        }
    }
}

@Composable
fun EntryScreen(role: String, vm: MainViewModel, mod: Modifier) {
    val projects by vm.projects.collectAsState()
    val allItems by vm.items.collectAsState()
    val entries by vm.entries.collectAsState()
    val settings by vm.settings.collectAsState()
    val type = typeFor(role)
    val ctx = LocalContext.current

    var projectPick by rememberSaveable { mutableStateOf("") }
    var itemCode by rememberSaveable { mutableStateOf("") }
    var qty by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Open") }
    var priority by rememberSaveable { mutableStateOf("Medium") }
    var reportDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var projectMenu by remember { mutableStateOf(false) }
    var itemMenu by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Entry?>(null) }

    val project = projectPick.ifBlank { settings.projectCode.ifBlank { projects.firstOrNull()?.code.orEmpty() } }
    val roleItems = itemsFor(role, allItems)
    val selected = allItems.firstOrNull { it.code == itemCode }
    val mine = entries.filter { it.role == role }

    if (showDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(reportDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton({
                    st.selectedDateMillis?.let { reportDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    showDate = false
                }) { Text("موافق") }
            },
            dismissButton = { TextButton({ showDate = false }) { Text("إلغاء") } }
        ) { DatePicker(state = st) }
    }

    if (showAdd) AddItemDialog(role, onDismiss = { showAdd = false }) { vm.addItem(it); itemCode = it.code; showAdd = false }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("حذف السجل") },
            text = { Text("هل تريد حذف سجل «${e.itemName}»؟ لا يمكن التراجع.") },
            confirmButton = { TextButton({ vm.delete(e); toDelete = null }) { Text("حذف") } },
            dismissButton = { TextButton({ toDelete = null }) { Text("إلغاء") } }
        )
    }

    LazyColumn(mod.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("إدخال بيانات — $role", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${settings.userName} — ${settings.userTitle}", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Box {
                OutlinedButton({ projectMenu = true }, Modifier.fillMaxWidth()) {
                    val n = projects.firstOrNull { it.code == project }
                    Text("المشروع: $project${if (n != null) " — " + n.nameAr.ifBlank { n.name } else ""}")
                }
                DropdownMenu(projectMenu, { projectMenu = false }) {
                    projects.forEach { x -> DropdownMenuItem({ Text("${x.code} — ${x.nameAr.ifBlank { x.name }}") }, { projectPick = x.code; projectMenu = false }) }
                }
            }
        }
        item { OutlinedButton({ showDate = true }, Modifier.fillMaxWidth()) { Text("تاريخ التقرير: $reportDate") } }
        item {
            Box {
                OutlinedButton({ itemMenu = true }, Modifier.fillMaxWidth()) {
                    Text(selected?.let { "${it.nameAr} (${it.unit})" } ?: "اختر البند أو المادة")
                }
                DropdownMenu(itemMenu, { itemMenu = false }) {
                    roleItems.forEach { x -> DropdownMenuItem({ Text("${x.nameAr} — ${x.code}") }, { itemCode = x.code; itemMenu = false }) }
                }
            }
        }
        item { TextButton({ showAdd = true }) { Text("+ إضافة بند جديد") } }
        item { OutlinedTextField(qty, { qty = it }, label = { Text("الكمية (إلزامي)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(hours, { hours = it }, label = { Text("الساعات — اختياري") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        if (type == "Issue") {
            item { Choice("الأولوية", priority, priorities) { priority = it } }
            item { Choice("الحالة", status, statuses) { status = it } }
        }
        item { OutlinedTextField(note, { note = it }, label = { Text("الملاحظات") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
        item {
            Button({
                val q = qty.trim().replace(',', '.').toDoubleOrNull()
                val h = hours.trim().replace(',', '.').toDoubleOrNull()
                when {
                    project.isBlank() -> Toast.makeText(ctx, "اختر المشروع أولاً", Toast.LENGTH_SHORT).show()
                    selected == null -> Toast.makeText(ctx, "اختر البند أولاً", Toast.LENGTH_SHORT).show()
                    q == null || q < 0 -> Toast.makeText(ctx, "أدخل كمية صحيحة", Toast.LENGTH_SHORT).show()
                    else -> {
                        vm.save(role, type, project, selected, q, h, if (type == "Issue") status else "", if (type == "Issue") priority else "", note.trim(), reportDate)
                        qty = ""; hours = ""; note = ""
                    }
                }
            }, Modifier.fillMaxWidth()) { Text("حفظ ومراجعة السجل") }
        }
        item {
            HorizontalDivider()
            Text("السجلات المحفوظة (${mine.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(mine.take(30), key = { it.id }) { e ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${e.itemName.ifBlank { "بلاغ/تحديث" }} — ${e.quantity ?: ""} ${e.unit}", fontWeight = FontWeight.Bold)
                    Text("${e.personName.ifBlank { "بدون اسم" }} — ${e.personTitle.ifBlank { "بدون صفة" }}", style = MaterialTheme.typography.bodySmall)
                    Text("تاريخ التقرير: ${e.reportDate.ifBlank { e.eventAt.take(10) }} • سُجّل: ${e.eventAt}", style = MaterialTheme.typography.bodySmall)
                    if (e.notes.isNotBlank()) Text(e.notes, style = MaterialTheme.typography.bodySmall)
                    TextButton({ toDelete = e }) { Text("حذف") }
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(role: String, onDismiss: () -> Unit, onAdd: (Item) -> Unit) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("No.") }
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة بند جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nameAr, { nameAr = it }, label = { Text("اسم البند بالعربية") }, singleLine = true)
                OutlinedTextField(nameEn, { nameEn = it }, label = { Text("الاسم بالإنجليزية — اختياري") }, singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("الوحدة (m, m2, m3, ton, No.)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton({
                if (nameAr.isBlank() || unit.isBlank()) Toast.makeText(ctx, "اكتب اسم البند ووحدته", Toast.LENGTH_SHORT).show()
                else onAdd(Item("U-" + System.currentTimeMillis().toString(36).uppercase(), nameEn.trim().ifBlank { nameAr.trim() }, nameAr.trim(), unit.trim(), role))
            }) { Text("إضافة") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } }
    )
}

private fun share(c: Context, f: File) {
    val u = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", f)
    c.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, u)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "مشاركة ملف Excel الموحد"
        )
    )
}
