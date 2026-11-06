package com.newapp.fieldreport

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Values every company edits once from the Settings screen. */
data class Settings(
    val userName: String = "",
    val userTitle: String = "",
    val company: String = "",
    val orgCode: String = "",
    val projectCode: String = "",
    val projectName: String = "",
    val projectNameAr: String = ""
)

class MainViewModel(a: Application) : AndroidViewModel(a) {
    private val dao = AppDatabase.get(a).dao()
    private val prefs = a.getSharedPreferences("fieldreport", Context.MODE_PRIVATE)
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val projects = dao.projects().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val items = dao.items().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val entries = dao.entries().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings

    /** Time of the last export (yyyy-MM-dd HH:mm), used to warn about records that were never exported. */
    private val _lastExport = MutableStateFlow(prefs.getString("lastExportAt", "").orEmpty())
    val lastExport: StateFlow<String> = _lastExport

    /** Called after a message was shown, so the same message can be shown again later. */
    fun clearMessage() { _message.value = "" }

    init {
        // First run: give the app a generic project and a starter item list for every role.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (dao.projectCount() == 0 && _settings.value.projectCode.isBlank()) dao.projects(Seed.projects)
                if (dao.itemCount() == 0) dao.items(Seed.items)
            } catch (e: Exception) {
                _message.value = "تعذر تجهيز البيانات الأولية: ${e.message}"
            }
        }
    }

    private fun loadSettings() = Settings(
        prefs.getString("userName", "").orEmpty(),
        prefs.getString("userTitle", "").orEmpty(),
        prefs.getString("company", "").orEmpty(),
        prefs.getString("orgCode", "").orEmpty(),
        prefs.getString("projectCode", "").orEmpty(),
        prefs.getString("projectName", "").orEmpty(),
        prefs.getString("projectNameAr", "").orEmpty()
    )

    fun saveSettings(s: Settings) {
        val old = _settings.value.projectCode.ifBlank { Seed.projects.first().code }
        prefs.edit()
            .putString("userName", s.userName).putString("userTitle", s.userTitle)
            .putString("company", s.company).putString("orgCode", s.orgCode)
            .putString("projectCode", s.projectCode).putString("projectName", s.projectName)
            .putString("projectNameAr", s.projectNameAr)
            .apply()
        _settings.value = s
        if (s.projectCode.isNotBlank()) viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.projects(listOf(Project(s.projectCode, s.projectName.ifBlank { s.projectCode }, s.projectNameAr.ifBlank { s.projectName.ifBlank { s.projectCode } })))
                if (old != s.projectCode) dao.deleteProjectIfUnused(old)
            } catch (e: Exception) {
                _message.value = "تعذر حفظ المشروع: ${e.message}"
            }
        }
        _message.value = "تم حفظ الإعدادات"
    }

    fun importFile(r: ContentResolver, u: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val b = XlsxImport.read(r, u)
                val p = b.sheets["Projects"].orEmpty().drop(1)
                    .filter { it.isNotEmpty() && it[0].isNotBlank() }
                    .map { Project(it[0], it.getOrNull(1).orEmpty(), it.getOrNull(2).orEmpty()) }
                val i = b.sheets["Items"].orEmpty().drop(1)
                    .filter { it.isNotEmpty() && it[0].isNotBlank() }
                    .map { Item(it[0], it.getOrNull(1).orEmpty(), it.getOrNull(2).orEmpty(), it.getOrNull(3).orEmpty(), it.getOrNull(4).orEmpty()) }
                val rows = b.sheets["Entries"].orEmpty().drop(1).filter { it.isNotEmpty() && it[0].isNotBlank() }
                if (p.isEmpty() && i.isEmpty() && rows.isEmpty()) {
                    _message.value = "الملف لا يحتوي بيانات مقروءة. تأكد أن أسماء الأوراق Projects وItems وEntries."
                    return@launch
                }
                if (p.isNotEmpty()) dao.projects(p)
                if (i.isNotEmpty()) dao.items(i)
                rows.forEach { dao.entry(rowToEntry(it)) }
                _message.value = "تم الاستيراد: ${p.size} مشاريع، ${i.size} بنود، ${rows.size} سجلات"
            } catch (e: Exception) {
                _message.value = "تعذر استيراد الملف: ${e.message}"
            }
        }
    }

    /** Excel turns a typed date into a serial number; convert it back to yyyy-MM-dd HH:mm. */
    private fun excelDate(s: String): String {
        val d = s.toDoubleOrNull() ?: return s
        if (d < 20000) return s
        val days = d.toLong()
        val secs = Math.round((d - days) * 86400)
        return LocalDate.of(1899, 12, 30).plusDays(days).atStartOfDay().plusSeconds(secs).format(fmt)
    }

    /** Same idea for a date-only column: Excel serial number -> yyyy-MM-dd. */
    private fun excelDay(s: String): String {
        val d = s.toDoubleOrNull() ?: return s.take(10)
        if (d < 20000) return s
        return LocalDate.of(1899, 12, 30).plusDays(d.toLong()).toString()
    }

    private fun rowToEntry(r: List<String>) = Entry(
        id = r.getOrNull(0).orEmpty(), role = r.getOrNull(1).orEmpty(), recordType = r.getOrNull(2).orEmpty(),
        projectCode = r.getOrNull(3).orEmpty(), itemCode = r.getOrNull(4).orEmpty(), itemName = r.getOrNull(5).orEmpty(),
        unit = r.getOrNull(6).orEmpty(), quantity = r.getOrNull(7)?.toDoubleOrNull(), hours = r.getOrNull(8)?.toDoubleOrNull(),
        status = r.getOrNull(9).orEmpty(), priority = r.getOrNull(10).orEmpty(), personCode = r.getOrNull(11).orEmpty(),
        personName = r.getOrNull(12).orEmpty(), eventAt = excelDate(r.getOrNull(13).orEmpty()), notes = r.getOrNull(14).orEmpty(),
        personTitle = r.getOrNull(15).orEmpty(), reportDate = excelDay(r.getOrNull(16).orEmpty()),
        latitude = r.getOrNull(17)?.toDoubleOrNull(), longitude = r.getOrNull(18)?.toDoubleOrNull(),
        photoRef = r.getOrNull(19).orEmpty()
    )

    fun save(
        role: String, type: String, project: String, item: Item, qty: Double?, hours: Double?,
        status: String, priority: String, note: String, reportDate: String
    ) {
        val now = LocalDateTime.now().format(fmt)
        val s = _settings.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.entry(
                    Entry(
                        id = UUID.randomUUID().toString(), role = role, recordType = type, projectCode = project,
                        itemCode = item.code, itemName = item.nameAr.ifBlank { item.name }, unit = item.unit,
                        quantity = qty, hours = hours, status = status, priority = priority, personCode = "",
                        personName = s.userName, eventAt = now, notes = note,
                        personTitle = s.userTitle, reportDate = reportDate
                    )
                )
                _message.value = "تم الحفظ — $now"
            } catch (e: Exception) {
                _message.value = "تعذر الحفظ: ${e.message}"
            }
        }
    }

    fun addItem(item: Item) {
        viewModelScope.launch(Dispatchers.IO) {
            try { dao.item(item); _message.value = "تمت إضافة البند" }
            catch (e: Exception) { _message.value = "تعذرت إضافة البند: ${e.message}" }
        }
    }

    fun delete(e: Entry) {
        viewModelScope.launch(Dispatchers.IO) {
            try { dao.deleteEntry(e); _message.value = "تم حذف السجل" }
            catch (x: Exception) { _message.value = "تعذر الحذف: ${x.message}" }
        }
    }

    /**
     * Exports the records of one report day (yyyy-MM-dd), or every record when [onlyDate] is null.
     * Returns null (and shows a message) when there is nothing to export.
     */
    suspend fun export(onlyDate: String? = null): File? = withContext<File?>(Dispatchers.IO) {
        val all = entries.value
        val list = if (onlyDate == null) all
                   else all.filter { it.reportDate.ifBlank { it.eventAt.take(10) } == onlyDate }
        if (list.isEmpty()) {
            _message.value = "لا توجد سجلات لتصديرها"
            return@withContext null
        }
        val dir = File(getApplication<Application>().cacheDir, "exports")
        dir.mkdirs()
        // Remove old export files (older than one day) so the phone cache does not fill up.
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000L }?.forEach { it.delete() }
        val now = LocalDateTime.now()
        val stamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        val code = _settings.value.projectCode.replace(Regex("[^A-Za-z0-9_-]"), "").ifBlank { "PRJ" }
        val tag = onlyDate?.replace("-", "") ?: "ALL"
        val f = File(dir, "FieldReport_${code}_${tag}_$stamp.xlsx")
        XlsxWriter.write(f, projects.value, items.value, list)
        val nowText = now.format(fmt)
        prefs.edit().putString("lastExportAt", nowText).apply()
        _lastExport.value = nowText
        f
    }
}
