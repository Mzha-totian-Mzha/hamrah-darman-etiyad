package ir.mosafir.recovery.data

import java.util.Calendar

/**
 * موتور پروتکل DST — دقیقاً بر پایه دو سند مرجع:
 *  ۱) «اصلاحیه پروتکل DST» قابل اجرا از ۱۳۹۸/۳/۱ ← اعداد اجرایی (برنامه‌های ۶–۳ و ۹–۱۶٫۵ و هروئین،
 *     جدول محرک‌ها، جدول تیپرینگ OT، نسبت‌های معادل‌سازی، ۲۲ روز توقف)
 *  ۲) «راهنما و راهنمایی در کنگره ۶۰» — ویرایش ۱۳۹۹ (صفحات ۴۱ تا ۶۷) ← چارچوب متد و پیوست ۱
 *
 *  D = ضریب ۰٫۸ (Dezhakam) | S = پله ۲۱ روزه | T = حداقل ۱۰ ماه درمان
 *  همه مقادیر بر حسب سی‌سی (cc) شربت اپیوم تینکچر (OT) است:
 *    هر ۱cc OT = ۱۰۰ میلی‌گرم تریاک خوراکی  |  ۱ گرم تریاک خوراکی = ۱۰cc
 *
 *  نکات تأییدشده با اسناد:
 *   • توقف روی سقف (تثبیت) در برنامه‌های ۶–۳، هروئین و جایگزینی متادون: «۲۲ روز یا دو پله»
 *     (اصلاحیه این عدد را سه بار آورده است) ← مبنای این موتور ۲۲ روز است.
 *   • سقف هروئین: ۳ × ۳٫۵ = ۱۰٫۵cc (عدد «۱۱/۵» اسکن، خطای تایپی است؛ راهنمای ویرایش ۹۹ ص۵۰ و ص۶۵)
 *   • تیپر الکل: ۶۱ روز و سقف OT الکل: ۶cc (اصلاحیه بخش ۸ ≈ «تیپر دو ماهه» ویرایش ۹۹ ص۵۷)
 *   • ترامادول: هر قرص ۱۰۰mg = ۱٫۵cc و قرص ۲۰۰mg = ۳cc (اصلاحیه بخش ۶ و پانویس آن)
 *
 *  توجه: این موتور جنبه آموزشی و کمکی دارد و جایگزین تشخیص راهنما/پزشک نیست.
 */
object Protocol {

    /** ضریب کاهش/افزایش هر پله (D = Dezhakam) */
    const val DST_COEFFICIENT = 0.8

    /** مدت هر پله (S = Step) */
    const val STEP_DAYS = 21

    /** مدت توقف تثبیت روی سقف — عبارت اصلاحیه: «۲۲ روز یا دو پله» */
    const val STABILIZE_DAYS = 22

    /** حداقل طول درمان (T = Time) */
    const val MIN_TREATMENT_MONTHS = 10

    // ---------------------------------------------------------------
    // جدول‌های مرجع اسناد
    // ---------------------------------------------------------------

    /**
     * جدول «الگوی تیپرینگ OT متد DST (حداکثر ۱۶٫۵ سی‌سی)» — پیوست ۱ ویرایش ۹۹ / اصلاحیه.
     * مدت هر پله طبق تعریف DST، ۲۱ روز است؛ ستون «دوره صدور نامه» جدول (هفتگی تا شش‌هفته‌ای)
     * مربوط به تجویز دارو است و در یادداشتِ هر پله آمده است.
     * پله آخر طبق توضیح ۴ می‌تواند ۲ تا ۲۱ روز ادامه داشته باشد (اینجا ۷ روز مبناست).
     */
    val OT_LADDER: List<PlanStep> = listOf(
        PlanStep("پله اول", StepKind.REDUCE, 3, 5.5, STEP_DAYS, "نامه هفتگی — نامه اول ۸ روز (۱۳۲cc)"),
        PlanStep("پله دوم", StepKind.REDUCE, 3, 4.5, STEP_DAYS, "نامه هفتگی (۹۴٫۵cc)"),
        PlanStep("پله سوم", StepKind.REDUCE, 3, 3.5, STEP_DAYS, "نامه هفتگی (۷۳٫۵cc)"),
        PlanStep("پله چهارم", StepKind.REDUCE, 3, 3.0, STEP_DAYS, "نامه ۲ هفته‌ای با صلاحدید راهنما (۶۳cc)"),
        PlanStep("پله پنجم", StepKind.REDUCE, 3, 2.5, STEP_DAYS, "نامه ۲ هفته‌ای با صلاحدید راهنما (۵۲٫۵cc)"),
        PlanStep("پله ششم", StepKind.REDUCE, 3, 2.0, STEP_DAYS, "نامه ۲ هفته‌ای با صلاحدید راهنما (۴۲cc)"),
        PlanStep("پله هفتم", StepKind.REDUCE, 3, 1.5, STEP_DAYS, "نامه ۳ هفته‌ای با صلاحدید راهنما (۳۱٫۵cc)"),
        PlanStep("پله هشتم", StepKind.REDUCE, 3, 1.2, STEP_DAYS, "نامه ۳ هفته‌ای با صلاحدید راهنما (۲۵٫۲cc)"),
        PlanStep("پله نهم", StepKind.REDUCE, 3, 1.0, STEP_DAYS, "نامه ۳ هفته‌ای با صلاحدید راهنما (۲۱cc)"),
        PlanStep("پله دهم", StepKind.REDUCE, 3, 0.8, STEP_DAYS, "نامه ۳ هفته‌ای با صلاحدید راهنما (۱۶٫۸cc)"),
        PlanStep("پله یازدهم", StepKind.REDUCE, 3, 0.6, STEP_DAYS, "نامه ۳ هفته‌ای با صلاحدید راهنما (۱۲٫۶cc)"),
        PlanStep("پله دوازدهم", StepKind.REDUCE, 3, 0.4, STEP_DAYS, "نامه ۶ هفته‌ای با صلاحدید راهنما (۸٫۴cc)"),
        PlanStep("پله سیزدهم", StepKind.REDUCE, 3, 0.3, STEP_DAYS, "نامه ۶ هفته‌ای با صلاحدید راهنما (۶٫۳cc)"),
        PlanStep("پله چهاردهم (BID)", StepKind.REDUCE, 2, 0.3, STEP_DAYS, "حذف یک وعده — نامه ۶ هفته‌ای (۴٫۲cc)"),
        PlanStep("پله پانزدهم — آخر (DAILY)", StepKind.FINAL, 1, 0.3, 7, "یک وعده ۰٫۳cc؛ ۲ تا ۲۱ روز و سپس دستور قطع")
    )

    /**
     * جدول «پروتکل انواع محرک‌ها: شیشه، حشیش، گل، ماری‌جوانا، کوکائین، قرص اکستازی و قرص B2» — اصلاحیه بخش ۴.
     * هفته اول و دوم ۷ روز؛ هر «پله» طبق تعریف DST، ۲۱ روز؛ هفته آخر ۲ تا ۲۱ روز.
     */
    val STIMULANT_LADDER: List<PlanStep> = listOf(
        PlanStep("هفته اول", StepKind.START, 1, 0.3, 7, "یک وعده ۰٫۳cc (Daily)"),
        PlanStep("هفته دوم", StepKind.START, 2, 0.3, 7, "دو وعده ۰٫۳cc (BID)"),
        PlanStep("پله اول", StepKind.BUILDUP, 3, 0.3, STEP_DAYS, "روزانه ۰٫۹cc"),
        PlanStep("پله دوم", StepKind.BUILDUP, 3, 0.4, STEP_DAYS, "روزانه ۱٫۲cc"),
        PlanStep("پله سوم", StepKind.BUILDUP, 3, 0.6, STEP_DAYS, "روزانه ۱٫۸cc"),
        PlanStep("پله چهارم", StepKind.BUILDUP, 3, 0.8, STEP_DAYS, "روزانه ۲٫۴cc"),
        PlanStep("پله پنجم", StepKind.BUILDUP, 3, 1.0, STEP_DAYS, "روزانه ۳cc"),
        PlanStep("پله ششم", StepKind.BUILDUP, 3, 1.2, STEP_DAYS, "روزانه ۳٫۶cc"),
        PlanStep("پله هفتم", StepKind.BUILDUP, 3, 1.5, STEP_DAYS, "روزانه ۴٫۵cc"),
        PlanStep("پله هشتم (تثبیت)", StepKind.STABILIZE, 3, 1.5, STEP_DAYS, "روزانه ۴٫۵cc — توقف روی سقف"),
        PlanStep("پله نهم", StepKind.REDUCE, 3, 1.2, STEP_DAYS, "روزانه ۳٫۶cc"),
        PlanStep("پله دهم", StepKind.REDUCE, 3, 1.0, STEP_DAYS, "روزانه ۳cc"),
        PlanStep("پله یازدهم", StepKind.REDUCE, 3, 0.8, STEP_DAYS, "روزانه ۲٫۴cc"),
        PlanStep("پله دوازدهم", StepKind.REDUCE, 3, 0.6, STEP_DAYS, "روزانه ۱٫۸cc"),
        PlanStep("پله سیزدهم", StepKind.REDUCE, 3, 0.4, STEP_DAYS, "روزانه ۱٫۲cc"),
        PlanStep("پله چهاردهم", StepKind.REDUCE, 3, 0.3, STEP_DAYS, "روزانه ۰٫۹cc"),
        PlanStep("پله پانزدهم", StepKind.REDUCE, 2, 0.3, STEP_DAYS, "BID — روزانه ۰٫۶cc"),
        PlanStep("هفته آخر", StepKind.FINAL, 1, 0.3, 7, "Daily — ۲ تا ۲۱ روز")
    )

    /** توضیحهای ۴گانه ذیل جدول تیپرینگ OT (پیوست ۱ ویرایش ۹۹ و اصلاحیه) */
    val OT_LADDER_NOTES = listOf(
        "صلاحدید در دوره صدور نامه فقط برای رهجوی مرتب، منظم و بدون غیبت است؛ در غیر این صورت دارو هفتگی تجویز می‌شود.",
        "تلورانس هر نامه (هر چند هفته) حداکثر ۳ سی‌سی است.",
        "نامه هفته اول رهجو برای ۸ روز محاسبه می‌شود.",
        "پله آخر (روزانه ۰٫۳ سی‌سی OT) می‌تواند ۲ تا ۲۱ روز ادامه داشته باشد."
    )

    // ---------------------------------------------------------------
    // ساخت برنامه شخصی بر اساس نوع ماده
    // ---------------------------------------------------------------

    fun buildPlan(settings: UserSettings): PersonalPlan = when (settings.substance) {
        SubstanceType.OPIUM_SMOKED -> plan356()
        SubstanceType.SOKHTE -> plan356()
        SubstanceType.OPIUM_ORAL -> planOralOpium(settings.consumption)
        SubstanceType.HEROIN -> planHeroin()
        SubstanceType.STIMULANT -> planStimulant()
        SubstanceType.METHADONE -> planMethadone(settings.consumption)
        SubstanceType.TRAMADOL -> planTramadol(settings.consumption)
        SubstanceType.SHIREH_ORAL -> planShirehOral(settings.consumption)
        SubstanceType.ALCOHOL -> planAlcohol()
        SubstanceType.SLEEPING -> planGuidance(SubstanceType.SLEEPING)
        SubstanceType.OTHER -> planGuidance(SubstanceType.OTHER)
    }

    /** ۱) تریاک کشیدنی، شیره کشیدنی و کرک — «برنامه ۶–۳» اصلاحیه (بخش ۱) */
    private fun plan356(): PersonalPlan {
        val steps = mutableListOf(
            PlanStep(
                "شروع — روزهای نخست", StepKind.START, 3, 1.0, 3,
                "هر وعده ۱cc (روزانه ۳cc)؛ پس از گذشت ۳ روز، مقدار دو برابر می‌شود"
            ),
            PlanStep(
                "پله صعودی", StepKind.BUILDUP, 3, 2.0, STEP_DAYS,
                "هر وعده ۲cc (روزانه ۶cc)"
            ),
            PlanStep(
                "افزایش تا سقف", StepKind.BUILDUP, 3, 2.5, STEP_DAYS,
                "هر وعده ۲٫۵cc (روزانه ۷٫۵cc) — تقسیم ۶ بر ضریب ۰٫۸"
            ),
            PlanStep(
                "تثبیت روی سقف", StepKind.STABILIZE, 3, 2.5, STABILIZE_DAYS,
                "سقف برنامه ۶–۳: هر وعده ۲٫۵cc (روزانه ۷٫۵cc) — «۲۲ روز یا دو پله» توقف"
            )
        )
        steps += ladderFrom(7.5)
        return PersonalPlan(
            SubstanceType.OPIUM_SMOKED,
            steps,
            startDailyCc = 3.0,
            capDailyCc = 7.5,
            notes = listOf(
                "برنامه ۶–۳ اصلاحیه ۱۳۹۸: شروع با هر وعده ۱cc، دو برابر کردن پس از ۳ روز، صعود تا سقف ۷٫۵cc و پس از ۲۲ روز توقف، کاهش طبق جدول.",
                "مصرف کشیدنی بلافاصله قطع و همزمان OT شروع می‌شود؛ برای تریاک، شیره کشیدنی، سوخته و کرک به هر مقدار و هر شیوه همین برنامه اجرا می‌شود."
            )
        )
    }

    /** ۲) تریاک خوراکی — معادل‌سازی (≤۱٫۶۵ گرم) یا برنامه ۹–۱۶٫۵ (خاخام) */
    private fun planOralOpium(grams: Double): PersonalPlan {
        val g = if (grams > 0.0) grams else 1.0
        return if (g <= 1.65) {
            val daily = round1(g * 10.0) // ۱۰۰ میلی‌گرم تریاک = ۱cc OT
            val steps = mutableListOf(
                PlanStep(
                    "پله اول — معادل‌سازی و TDS", StepKind.START, 3, round2(daily / 3.0), STEP_DAYS,
                    "مقدار معادل به سه وعده مساوی تقسیم و یک پله ۲۱ روزه ثابت می‌ماند"
                )
            )
            if (daily < 16.5) {
                val up = round1((daily / DST_COEFFICIENT).coerceAtMost(16.5))
                steps += PlanStep(
                    "پله افزایشی (اختیاری)", StepKind.BUILDUP, 3, round2(up / 3.0), STEP_DAYS,
                    "در صورت کمتر بودن از سقف، به تشخیص راهنما می‌توان یک پله افزایش داد (تقسیم بر ۰٫۸)"
                )
            }
            steps += PlanStep(
                "تکرار پله (اختیاری)", StepKind.STABILIZE, 3,
                round2(steps.last().perDoseCc), STEP_DAYS,
                "پس از تکرار، عملیات تیپرینگ با ضریب ۰٫۸ آغاز می‌شود"
            )
            steps += ladderFrom(daily)
            PersonalPlan(
                SubstanceType.OPIUM_ORAL, steps,
                startDailyCc = daily, capDailyCc = 16.5,
                notes = listOf(
                    "مصرف ≤ ۱٫۶۵ گرم: هر ۱۰۰ میلی‌گرم تریاک = ۱cc شربت OT؛ مقدار معادل TDS و پله اول برنامه است.",
                    "پس از یک پله ۲۱ روزه می‌توان تکرار پله و تیپرینگ با ضریب ۰٫۸ را آغاز کرد؛ رُند کردن دقیق مقدار با نظر راهنما انجام می‌شود."
                )
            )
        } else {
            // برنامه ۹–۱۶٫۵ (خاخام): جزئیات افزایش در ویرایش ۹۹ ص۴۹ («حالت دوم»)
            val steps = mutableListOf(
                PlanStep(
                    "شروع برنامه ۹–۱۶٫۵", StepKind.START, 3, 3.0, 3,
                    "سه روز اول: هر وعده ۳cc (روزانه ۹cc)"
                ),
                PlanStep(
                    "رسیدن به سقف", StepKind.BUILDUP, 3, 5.5, STEP_DAYS,
                    "پس از ۳ روز: هر وعده ۵٫۵cc (روزانه ۱۶٫۵cc)"
                ),
                PlanStep(
                    "تثبیت روی سقف", StepKind.STABILIZE, 3, 5.5, STABILIZE_DAYS,
                    "یک پله تکرار روی سقف ۱۶٫۵cc — «۲۲ روز یا دو پله»"
                )
            )
            steps += ladderFrom(16.5)
            PersonalPlan(
                SubstanceType.OPIUM_ORAL, steps,
                startDailyCc = 9.0, capDailyCc = 16.5,
                notes = listOf(
                    "مصرف > ۱٫۶۵ گرم: برنامه ۹–۱۶٫۵ (خاخام) اجرا می‌شود.",
                    "طبق ویرایش ۹۹ (ص۴۹): سه روز روزانه ۹cc، سپس سقف ۱۶٫۵cc، یک پله تکرار و بعد تیپرینگ با ضریب ۰٫۸ طبق جدول OT."
                )
            )
        }
    }

    /** ۳) هروئین — اصلاحیه ۱۳۹۸ (بخش ۳): شروع مستقیم روزانه ۶cc و سقف ۱۰٫۵cc */
    private fun planHeroin(): PersonalPlan {
        val steps = mutableListOf(
            PlanStep(
                "شروع", StepKind.START, 3, 2.0, STEP_DAYS,
                "از روز نخست هر وعده ۲cc (روزانه ۶cc)؛ ۲۱ روز ثابت"
            ),
            PlanStep(
                "افزایش تا سقف", StepKind.BUILDUP, 3, 3.5, STEP_DAYS,
                "هر وعده ۳٫۵cc (روزانه ۱۰٫۵cc)"
            ),
            PlanStep(
                "تثبیت روی سقف", StepKind.STABILIZE, 3, 3.5, STABILIZE_DAYS,
                "«۲۲ روز یا دو پله» توقف و بعد کاهش طبق جدول تا صفر"
            )
        )
        steps += ladderFrom(10.5)
        return PersonalPlan(
            SubstanceType.HEROIN, steps,
            startDailyCc = 6.0, capDailyCc = 10.5,
            notes = listOf(
                "اصلاحیه ۱۳۹۸: شروع از روز نخست با روزانه ۶cc؛ سقف برنامه ۳ × ۳٫۵ = ۱۰٫۵cc است (عدد «۱۱/۵» در اسکن، خطای تایپی است و با سقف ویرایش ۹۹ ص۵۰ و ص۶۵ سازگار نیست).",
                "هروئین به‌صورت تدریجی قابل تیپر نیست؛ بلافاصله قطع و با OT جایگزین و سپس طبق جدول کاهش داده می‌شود."
            )
        )
    }

    /** ۴) محرک‌ها — جدول ۱۷ ردیفی اصلاحیه (بخش ۴) */
    private fun planStimulant(): PersonalPlan = PersonalPlan(
        SubstanceType.STIMULANT,
        STIMULANT_LADDER,
        startDailyCc = 0.3,
        capDailyCc = 4.5,
        notes = listOf(
            "شیشه، حشیش، گل، ماری‌جوانا، کوکائین، قرص اکستازی و قرص B2 همه با همین جدول درمان می‌شوند.",
            "هفته اول یک وعده ۰٫۳cc، هفته دوم دو وعده (BID) و از پله اول سه وعده (TDS).",
            "پله تثبیت، پله هشتم با روزانه ۴٫۵cc است؛ هفته آخر ۲ تا ۲۱ روز ادامه دارد.",
            "طبق متد DST: در مصرف همزمان چند ماده، مصرف غالب ملاک تجویز OT است؛ الکل پروتکل جداگانه دارد و ۶۱ روز تیپر می‌شود (اصلاحیه پروتکل — بند درمان الکل)."
        )
    )

    /** ۵) متادون — جایگزینی هفتگی وعده‌به‌وعده (حالت اول و دوم اصلاحیه — بخش ۵) */
    private fun planMethadone(ccDaily: Double): PersonalPlan {
        return if (ccDaily <= 16.5) {
            val daily = if (ccDaily > 0.0) round1(ccDaily) else 10.0
            val per = round2(daily / 3.0)
            val steps = mutableListOf(
                PlanStep(
                    "TDS شربت متادون", StepKind.START, 3, per, 7,
                    "مصرف به سه وعده مساوی تقسیم می‌شود؛ یک هفته جهت سازگاری (می‌توان برای رُند شدن طبق جدول گرد کرد)"
                ),
                PlanStep(
                    "جایگزینی وعده ظهر", StepKind.BUILDUP, 3, per, 7,
                    "صبح متادون · ظهر OT · شب متادون"
                ),
                PlanStep(
                    "جایگزینی وعده صبح", StepKind.BUILDUP, 3, per, 7,
                    "صبح OT · ظهر OT · شب متادون"
                ),
                PlanStep(
                    "جایگزینی وعده شب و تثبیت", StepKind.STABILIZE, 3, per, STABILIZE_DAYS,
                    "هر سه وعده OT — «۲۲ روز یا دو پله» توقف جهت سازگاری"
                )
            )
            steps += ladderFrom(daily)
            PersonalPlan(
                SubstanceType.METHADONE, steps,
                startDailyCc = daily, capDailyCc = 16.5,
                notes = listOf(
                    "حالت اول اصلاحیه (≤ ۱۶٫۵cc): دقیقاً به همان میزان متادون، OT تجویز و وعده‌به‌وعده (ظهر، صبح، شب) جایگزین می‌شود.",
                    "یک قرص متادون ۵ میلی‌گرمی = ۱cc شربت متادون = ۱cc شربت OT؛ قرص ۱۰mg = ۲cc و قرص ۲۰mg = ۸cc."
                )
            )
        } else {
            // حالت دوم: کاهش همزمان + TDS طی حداکثر دو هفته تا نزدیک ۱۶٫۵cc
            val per = round2(16.5 / 3.0)
            val steps = mutableListOf(
                PlanStep(
                    "کاهش همزمان و TDS متادون", StepKind.START, 3, per, 14,
                    "کاهش تدریجی و تقسیم به سه وعده تا نزدیک ۱۶٫۵cc طی حداکثر ۲ هفته"
                ),
                PlanStep(
                    "جایگزینی وعده ظهر", StepKind.BUILDUP, 3, per, 7,
                    "صبح متادون · ظهر OT · شب متادون"
                ),
                PlanStep(
                    "جایگزینی وعده صبح", StepKind.BUILDUP, 3, per, 7,
                    "صبح OT · ظهر OT · شب متادون"
                ),
                PlanStep(
                    "جایگزینی وعده شب و تثبیت", StepKind.STABILIZE, 3, per, STABILIZE_DAYS,
                    "هر سه وعده OT — «۲۲ روز یا دو پله» توقف"
                )
            )
            steps += ladderFrom(16.5)
            PersonalPlan(
                SubstanceType.METHADONE, steps,
                startDailyCc = 16.5, capDailyCc = 16.5,
                notes = listOf(
                    "حالت دوم اصلاحیه (> ۱۶٫۵cc): ابتدا طی حداکثر دو هفته به نزدیک ۱۶٫۵cc می‌رسیم، سپس مراحل جایگزینی OT."
                )
            )
        }
    }

    /** برنامهٔ راهنمایی برای موادی که جدول مستقل ندارند (خواب‌آور/سایر) — بدون دوز خودکار */
    private fun planGuidance(substance: SubstanceType): PersonalPlan {
        val isSleep = substance == SubstanceType.SLEEPING
        return PersonalPlan(
            substance,
            listOf(
                PlanStep(
                    "برنامه‌ریزی فقط با هماهنگی راهنما", StepKind.START, 3, 0.0, 1,
                    if (isSleep)
                        "برای قرص‌های خواب جدول محاسباتی مستقلی در متد DST ثبت نشده است؛ نوع دارو، مقدار و زمان‌بندی را فقط راهنمای تو تعیین می‌کند."
                    else
                        "برای این ماده برنامهٔ محاسباتی پیش‌فرضی وجود ندارد؛ نوع ماده و برنامهٔ درمان را فقط با هماهنگی راهنمای خودت ثبت کن."
                )
            ),
            startDailyCc = 0.0, capDailyCc = 0.0,
            notes = listOf(
                if (isSleep)
                    "«راهنما و راهنمایی»: قرص‌های خواب و آرام‌بخش هم روی سیستم ایکس اثر می‌گذارند (آنتی‌اکس)؛ درمان اعتیاد با جایگزینی این داروها نتیجه‌بخش نبوده است."
                else
                    "متد DST برای تریاک، شیره، سوخته، هروئین، متادون، ترامادول، الکل و محرک‌ها جدول دارد؛ بقیهٔ مواد فقط با دستور راهنما."
            )
        )
    }

    /** ۶) ترامادول — «دقیقاً مانند پروتکل متادون»؛ هر قرص ۱۰۰mg = ۰٫۵cc OT (پانویس اصلاحیهٔ بخش ۶ و جدول راهنما) */
    private fun planTramadol(pills: Double): PersonalPlan {
        val pillsCount = if (pills > 0.0) pills else 4.0
        val computed = round2(pillsCount * 0.5) // قرص ۱۰۰mg = ۰٫۵cc ← قرص ۲۰۰mg = ۱cc
        val daily = computed.coerceIn(6.0, 16.5)
        val raised = computed < 6.0
        val per = round2(daily / 3.0)
        val steps = mutableListOf(
            PlanStep(
                "TDS قرص ترامادول", StepKind.START, 3, per, 7,
                "معادل OT به سه وعده مساوی تقسیم می‌شود؛ یک هفته جهت سازگاری"
            ),
            PlanStep(
                "جایگزینی وعده ظهر", StepKind.BUILDUP, 3, per, 7,
                "صبح ترامادول · ظهر OT · شب ترامادول"
            ),
            PlanStep(
                "جایگزینی وعده صبح", StepKind.BUILDUP, 3, per, 7,
                "صبح OT · ظهر OT · شب ترامادول"
            ),
            PlanStep(
                "جایگزینی وعده شب و تثبیت", StepKind.STABILIZE, 3, per, STABILIZE_DAYS,
                "هر سه وعده OT — «۲۲ روز یا دو پله» توقف"
            )
        )
        steps += ladderFrom(daily)
        return PersonalPlan(
            SubstanceType.TRAMADOL, steps,
            startDailyCc = daily, capDailyCc = 16.5,
            notes = listOf(
                "هر قرص ترامادول ۱۰۰ میلی‌گرمی = ۰٫۵cc شربت OT و قرص ۲۰۰ میلی‌گرمی = ۱cc (پانویس اصلاحیهٔ بخش ۶ و جدول «راهنما و راهنمایی»).",
                "برنامه دقیقاً مانند متادون اجرا می‌شود (جایگزینی هفتگی وعده‌به‌وعده + ۲۲ روز تثبیت)." +
                    if (raised) " چون مقدار محاسبه‌شده کمتر از ۶cc بود، تا سقف ۶cc بالا برده می‌شود." else ""
            )
        )
    }

    /** ۷) شیره خوراکی — محاسبه مثل تریاک خوراکی + جدول جایگزینی هفتگی (اصلاحیه بخش ۷) */
    private fun planShirehOral(grams: Double): PersonalPlan {
        val g = if (grams > 0.0) grams else 1.0
        return if (g > 1.65) {
            // حالت دوم: کاهش و TDS طی حداکثر دو هفته تا نزدیک سقف
            val per = round2(16.5 / 3.0)
            val steps = mutableListOf(
                PlanStep(
                    "کاهش همزمان و TDS شیره", StepKind.START, 3, per, 14,
                    "کاهش تدریجی و تقسیم به سه وعده تا معادل ۱۶٫۵cc OT طی حداکثر ۲ هفته"
                ),
                PlanStep(
                    "جایگزینی وعده ظهر", StepKind.BUILDUP, 3, per, 7,
                    "صبح شیره · ظهر OT · شب شیره"
                ),
                PlanStep(
                    "جایگزینی وعده صبح", StepKind.BUILDUP, 3, per, 7,
                    "صبح OT · ظهر OT · شب شیره"
                ),
                PlanStep(
                    "جایگزینی وعده شب و تثبیت", StepKind.STABILIZE, 3, per, STABILIZE_DAYS,
                    "هر سه وعده OT — «۲۲ روز یا دو پله» توقف"
                )
            )
            steps += ladderFrom(16.5)
            PersonalPlan(
                SubstanceType.SHIREH_ORAL, steps,
                startDailyCc = 16.5, capDailyCc = 16.5,
                notes = listOf(
                    "معادلسازی شیره خوراکی دقیقاً مثل تریاک خوراکی است؛ چون معادل از سقف بیشتر است، ابتدا طی دو هفته به نزدیک ۱۶٫۵cc کاهش و سپس جایگزینی هفتگی انجام می‌شود."
                )
            )
        } else {
            val daily = round1(g * 10.0).coerceAtMost(16.5)
            val per = round2(daily / 3.0)
            val steps = mutableListOf(
                PlanStep(
                    "TDS شیره خوراکی", StepKind.START, 3, per, 7,
                    "مقدار شیره به سه وعده مساوی تقسیم می‌شود؛ یک هفته"
                ),
                PlanStep(
                    "جایگزینی وعده ظهر", StepKind.BUILDUP, 3, per, 7,
                    "صبح شیره · ظهر OT · شب شیره"
                ),
                PlanStep(
                    "جایگزینی وعده صبح", StepKind.BUILDUP, 3, per, 7,
                    "صبح OT · ظهر OT · شب شیره"
                ),
                PlanStep(
                    "جایگزینی وعده شب و تثبیت", StepKind.STABILIZE, 3, per, STABILIZE_DAYS,
                    "هر سه وعده OT — «۲۲ روز یا دو پله» توقف"
                )
            )
            steps += ladderFrom(daily)
            PersonalPlan(
                SubstanceType.SHIREH_ORAL, steps,
                startDailyCc = daily, capDailyCc = 16.5,
                notes = listOf(
                    "محاسبه دقیقاً مثل تریاک خوراکی است (هر ۱۰۰ میلی‌گرم = ۱cc OT)؛ جایگزینی، هفته‌ای یک وعده طبق جدول‌های چهار هفته‌ای اصلاحیه انجام می‌شود."
                )
            )
        }
    }

    /** ۸) الکل — تیپر ۶۱ روزه الکل، سپس متد شیشه با سقف ۶cc (اصلاحیه بخش ۸) */
    private fun planAlcohol(): PersonalPlan {
        val steps = mutableListOf(
            PlanStep(
                "تثبیت و TDS الکل", StepKind.START, 3, 0.0, 7,
                "مقدار الکل روزانه محاسبه و به سه وعده مساوی تقسیم می‌شود"
            ),
            PlanStep(
                "تیپر الکل", StepKind.START, 3, 0.0, 61,
                "طی ۶۱ روز الکل پله‌پله کم و سپس قطع می‌شود (اصلاحیه بخش ۸؛ ≈ «تیپر دو ماهه» ویرایش ۹۹)"
            )
        )
        // متد شیشه با سقف ۶cc: از جدول محرک‌ها تا ۴٫۵cc و سپس پله سقف ۶cc (هر وعده ۲cc — جدول OT پله ششم)
        steps += STIMULANT_LADDER.subList(2, 9) // پله اول تا هفتم (۰٫۹ تا ۴٫۵cc روزانه)
        steps += PlanStep(
            "پله سقف الکل", StepKind.STABILIZE, 3, 2.0, STABILIZE_DAYS,
            "سقف OT در درمان الکل: هر وعده ۲cc (روزانه ۶cc) — «۲۲ روز یا دو پله» توقف"
        )
        steps += ladderFrom(6.0)
        return PersonalPlan(
            SubstanceType.ALCOHOL, steps,
            startDailyCc = 0.3, capDailyCc = 6.0,
            notes = listOf(
                "فقط برای فردی که کاملاً به الکل معتاد شده و مرتب و منظم و حاضر در جلسات است، OT «همانند متد شیشه» جایگزین می‌شود.",
                "سقف OT در درمان الکل ۶ سی‌سی روزانه است؛ بین مصرف گاه‌به‌گاه و مصرف روزانه منظم تفاوت زیادی وجود دارد.",
                "قطع ناگهانی الکل در وابستگی شدید می‌تواند تشنج بیاورد؛ تیپر ۶۱ روزه را خودسرانه کوتاه نکن."
            )
        )
    }

    /** ورود به جدول کاهش OT از بزرگ‌ترین پله‌ای که جمع روزانه‌اش ≤ مقدار فرد باشد */
    private fun ladderFrom(dailyCc: Double): List<PlanStep> {
        val idx = OT_LADDER.indexOfFirst { it.dailyCc <= dailyCc + 0.001 }
        return if (idx < 0) OT_LADDER.takeLast(1) else OT_LADDER.subList(idx, OT_LADDER.size)
    }

    // ---------------------------------------------------------------
    // پیمایش زمان و پله‌ها
    // ---------------------------------------------------------------

    /** شماره پله جاری بر اساس روزِ سپری‌شده از شروع (null یعنی برنامه تمام شده) */
    fun currentStepIndex(plan: PersonalPlan, dayIndex: Long): Int? {
        var acc = 0L
        plan.steps.forEachIndexed { i, step ->
            acc += step.days
            if (dayIndex < acc) return i
        }
        return null
    }

    /** روز شروع پله (بر اساس اندیس) */
    fun stepStartDay(plan: PersonalPlan, index: Int): Long =
        plan.steps.take(index).sumOf { it.days.toLong() }

    /** نرمال‌سازی زمان به نیمه‌شب محلی */
    fun normalizeDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** تعداد روزهای کامل گذشته از شروع درمان */
    fun daysSinceStart(settings: UserSettings, nowMillis: Long): Long {
        val start = settings.startDateMillis ?: return 0L
        val diff = normalizeDay(nowMillis) - normalizeDay(start)
        return diff / 86_400_000L
    }

    /** ساعت وعده‌ها بر اساس مدل انتخابی (جدول «فاصله روزانه وعده‌ها در متد DST» — اصلاحیه) */
    fun doseTimes(model: Int): List<String> = when (model) {
        1 -> listOf("صبح — ۰۵:۰۰", "ظهر — ۱۳:۰۰", "شب — ۲۱:۰۰")
        3 -> listOf("صبح — ۰۷:۰۰", "ظهر — ۱۵:۰۰", "شب — ۲۳:۰۰")
        else -> listOf("صبح — ۰۶:۰۰", "ظهر — ۱۴:۰۰", "شب — ۲۲:۰۰")
    }

    /** مقدار مصرف پیش‌فرض هنگام انتخاب ماده (برای مواد نیازمند مقدار) */
    fun defaultConsumption(substance: SubstanceType): Double = when (substance) {
        SubstanceType.OPIUM_ORAL, SubstanceType.SHIREH_ORAL -> 1.0
        SubstanceType.METHADONE -> 10.0
        SubstanceType.TRAMADOL -> 4.0
        else -> 0.0
    }

    // ---------------------------------------------------------------
    // کمک‌تابع‌ها
    // ---------------------------------------------------------------

    private fun round1(v: Double) = kotlin.math.round(v * 10.0) / 10.0
    private fun round2(v: Double) = kotlin.math.round(v * 100.0) / 100.0
}
