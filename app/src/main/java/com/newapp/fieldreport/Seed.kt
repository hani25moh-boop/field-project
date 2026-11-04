package com.newapp.fieldreport

/** Generic starter data. Every company can edit the project from Settings and import its own items from Excel. */
object Seed {
    val projects = listOf(Project("PRJ-001", "My Project", "مشروعي"))

    private fun i(code: String, en: String, ar: String, unit: String, cat: String) = Item(code, en, ar, unit, cat)

    val items = listOf(
        // مدير المشروع
        i("PM-001", "Overall physical progress", "نسبة الإنجاز الكلية للمشروع", "%", "مدير المشروع"),
        i("PM-002", "Progress variance (planned vs actual)", "فرق الإنجاز بين المخطط والفعلي", "%", "مدير المشروع"),
        i("PM-003", "Client meetings held", "اجتماعات مع العميل", "No.", "مدير المشروع"),
        i("PM-004", "Variation orders raised", "أوامر التغيير المقدمة", "No.", "مدير المشروع"),
        i("PM-005", "Subcontractor payments approved", "مستخلصات مقاولي الباطن المعتمدة", "No.", "مدير المشروع"),
        i("PM-006", "Risks escalated", "مخاطر تم رفعها", "No.", "مدير المشروع"),
        // مدير الموقع
        i("SM-001", "Excavation", "أعمال الحفر", "m3", "مدير الموقع"),
        i("SM-002", "Plain concrete", "الخرسانة العادية", "m3", "مدير الموقع"),
        i("SM-003", "Reinforced concrete", "الخرسانة المسلحة", "m3", "مدير الموقع"),
        i("SM-004", "Rebar fixing", "تركيب حديد التسليح", "ton", "مدير الموقع"),
        i("SM-005", "Formwork", "أعمال الشدات الخشبية", "m2", "مدير الموقع"),
        i("SM-006", "Blockwork", "أعمال البلوك", "m2", "مدير الموقع"),
        i("SM-007", "Plastering", "أعمال اللياسة", "m2", "مدير الموقع"),
        i("SM-008", "Tiling", "أعمال البلاط", "m2", "مدير الموقع"),
        i("SM-009", "Painting", "أعمال الدهان", "m2", "مدير الموقع"),
        i("SM-010", "Waterproofing", "أعمال العزل المائي", "m2", "مدير الموقع"),
        // المخازن
        i("ST-001", "Cement received", "أسمنت وارد", "bag", "المخازن"),
        i("ST-002", "Rebar received", "حديد تسليح وارد", "ton", "المخازن"),
        i("ST-003", "Sand received", "رمل وارد", "m3", "المخازن"),
        i("ST-004", "Aggregate received", "حصى وارد", "m3", "المخازن"),
        i("ST-005", "Blocks received", "بلوك وارد", "No.", "المخازن"),
        i("ST-006", "Material issued to site", "مواد مصروفة للموقع", "No.", "المخازن"),
        i("ST-007", "Damaged or rejected material", "مواد تالفة أو مرفوضة", "No.", "المخازن"),
        i("ST-008", "Fuel received", "وقود وارد", "l", "المخازن"),
        // مهندس الكهرباء
        i("EL-001", "Conduit installation", "تمديد مواسير الكهرباء", "m", "مهندس الكهرباء"),
        i("EL-002", "Cable pulling", "سحب الكابلات", "m", "مهندس الكهرباء"),
        i("EL-003", "Cable tray installation", "تركيب سلالم الكابلات", "m", "مهندس الكهرباء"),
        i("EL-004", "Junction and switch boxes", "علب التوصيل والمفاتيح", "No.", "مهندس الكهرباء"),
        i("EL-005", "Lighting fixtures installed", "تركيب وحدات الإنارة", "No.", "مهندس الكهرباء"),
        i("EL-006", "Distribution boards installed", "تركيب لوحات التوزيع", "No.", "مهندس الكهرباء"),
        i("EL-007", "Earthing works", "أعمال التأريض", "m", "مهندس الكهرباء"),
        i("EL-008", "Insulation and continuity tests", "اختبارات العزل والاستمرارية", "No.", "مهندس الكهرباء"),
        // مهندس الميكانيك
        i("ME-001", "Water supply pipes", "مواسير تغذية المياه", "m", "مهندس الميكانيك"),
        i("ME-002", "Drainage pipes", "مواسير الصرف", "m", "مهندس الميكانيك"),
        i("ME-003", "Sanitary fixtures installed", "تركيب الأدوات الصحية", "No.", "مهندس الميكانيك"),
        i("ME-004", "Pumps installed", "تركيب المضخات", "No.", "مهندس الميكانيك"),
        i("ME-005", "HVAC ducting", "مجاري التكييف", "m", "مهندس الميكانيك"),
        i("ME-006", "AC units installed", "تركيب وحدات التكييف", "No.", "مهندس الميكانيك"),
        i("ME-007", "Fire fighting pipes", "مواسير مكافحة الحريق", "m", "مهندس الميكانيك"),
        i("ME-008", "Pressure tests", "اختبارات الضغط", "No.", "مهندس الميكانيك"),
        // الجودة
        i("QA-001", "Non-conformance report (NCR)", "تقرير عدم مطابقة", "No.", "الجودة"),
        i("QA-002", "Inspection requests approved", "طلبات فحص معتمدة", "No.", "الجودة"),
        i("QA-003", "Inspection requests rejected", "طلبات فحص مرفوضة", "No.", "الجودة"),
        i("QA-004", "Concrete cube samples", "عينات مكعبات الخرسانة", "No.", "الجودة"),
        i("QA-005", "Material approval requests", "طلبات اعتماد مواد", "No.", "الجودة"),
        i("QA-006", "Snag and defect items", "بنود ملاحظات وعيوب", "No.", "الجودة"),
        // السلامة
        i("HS-001", "Toolbox talk held", "اجتماع سلامة قبل العمل", "No.", "السلامة"),
        i("HS-002", "Near miss reported", "حادث وشيك مبلغ عنه", "No.", "السلامة"),
        i("HS-003", "Injury incident", "حادث إصابة", "No.", "السلامة"),
        i("HS-004", "PPE violation", "مخالفة معدات الحماية الشخصية", "No.", "السلامة"),
        i("HS-005", "Safety inspection", "جولة تفتيش سلامة", "No.", "السلامة"),
        i("HS-006", "Permit-to-work issued", "تصريح عمل صادر", "No.", "السلامة"),
        i("HS-007", "Housekeeping deficiencies", "ملاحظات النظافة والترتيب", "No.", "السلامة"),
        // الحسابات والمالية
        i("FN-001", "Invoices processed", "فواتير تمت معالجتها", "No.", "الحسابات والمالية"),
        i("FN-002", "Payments released", "دفعات تم صرفها", "No.", "الحسابات والمالية"),
        i("FN-003", "Petty cash expenses", "مصروفات نثرية", "No.", "الحسابات والمالية"),
        i("FN-004", "Subcontractor claims reviewed", "مستخلصات تمت مراجعتها", "No.", "الحسابات والمالية"),
        i("FN-005", "Payroll batches processed", "كشوف رواتب تمت معالجتها", "No.", "الحسابات والمالية"),
        // المشتريات
        i("PR-001", "Purchase orders issued", "أوامر شراء صادرة", "No.", "المشتريات"),
        i("PR-002", "Quotations received", "عروض أسعار واردة", "No.", "المشتريات"),
        i("PR-003", "Deliveries confirmed", "توريدات مؤكدة", "No.", "المشتريات"),
        i("PR-004", "Late deliveries", "توريدات متأخرة", "No.", "المشتريات"),
        i("PR-005", "Supplier evaluations", "تقييمات موردين", "No.", "المشتريات"),
        i("PR-006", "Pending material requests", "طلبات مواد معلقة", "No.", "المشتريات")
    )
}
