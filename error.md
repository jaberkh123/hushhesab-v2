علت اصلی Crash

این قسمت تعیین‌کننده است:

FATAL EXCEPTION: main
java.lang.IllegalStateException:
A migration from 1 to 2 was required but not found.

Please provide the necessary Migration path via
RoomDatabase.Builder.addMigration(...)
or allow for destructive migrations via
RoomDatabase.Builder.fallbackToDestructiveMigration*

یعنی دیتابیس Room اپلیکیشن شما قبلاً با نسخه‌ی 1 ساخته شده، ولی الان کد برنامه دیتابیس را نسخه‌ی 2 اعلام کرده است.

مثلاً احتمالاً چیزی شبیه این داری:

@Database(
    entities = [...],
    version = 2
)
abstract class AppDatabase : RoomDatabase()

ولی Room نمی‌داند چطور دیتابیس موجود را از:

1 → 2

تغییر دهد.

در نتیجه هنگام باز شدن دیتابیس:

SQLiteOpenHelper.onUpgrade()
       ↓
Room
       ↓
Migration 1 → 2 پیدا نشد
       ↓
IllegalStateException
       ↓
FATAL EXCEPTION
       ↓
Application killed

و دقیقاً چند خط بعد هم تأییدش می‌کند:

Force finishing activity
com.aistudio.kharjyar.app/com.example.MainActivity
راه‌حل ۱ — راه درست: Migration

اگر اطلاعات دیتابیس کاربر باید حفظ شود، Migration تعریف کن.

مثلاً اگر در نسخه 2 یک ستون اضافه کرده‌ای:

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0"
        )
    }
}

و هنگام ساخت Database:

Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "app_database"
)
    .addMigrations(MIGRATION_1_2)
    .build()

اگر Room جدیدتر استفاده می‌کنی و API پروژه‌ات با SQLiteDatabase/KTX متفاوت است، اصل موضوع همچنان همین است:

Migration(1, 2)

باید به Builder معرفی شود.

راه‌حل ۲ — اگر دیتابیس آزمایشی است

اگر این پروژه فعلاً Development است و اطلاعات دیتابیس مهم نیست، می‌توانی بگویی در صورت نبود Migration، دیتابیس حذف و دوباره ساخته شود:

Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "app_database"
)
    .fallbackToDestructiveMigration()
    .build()

این روش برای تست خیلی ساده است.

اما دقت کن:

fallbackToDestructiveMigration() می‌تواند اطلاعات دیتابیس موجود را پاک کند.

پس برای Production معمولاً راه‌حل مناسبی نیست مگر اینکه عمداً همین رفتار را بخواهی.

یک نکته مهم در لاگ تو

قبل از Crash چند خط مربوط به Emulator داری:

Could not open '/dev/goldfish_pipe_dprctd'
both vsock and goldfish_pipe paths failed

و:

mapper 4.x is not supported
mapper 3.x is not supported

و:

avc: denied ... gralloc.ranchu.so

و:

Skipped 62 frames!
The application may be doing too much work on its main thread.

این‌ها به احتمال زیاد علت اصلی کرش نیستند.

چون بعد از همه‌ی این‌ها اپلیکیشن با موفقیت به این نقطه می‌رسد:

Displayed com.aistudio.kharjyar.app/com.example.MainActivity for user 0: +2s671ms

و حدود یک ثانیه بعد Room کرش می‌کند:

21:18:48.461 FATAL EXCEPTION: main

بنابراین ترتیب اتفاقات تقریباً این است:

App Launch
   ↓
MainActivity ساخته می‌شود
   ↓
UI / GPU initialization
   ↓
Room Database باز می‌شود
   ↓
Database version = 2
Existing database version = 1
   ↓
Migration 1 → 2 وجود ندارد
   ↓
IllegalStateException
   ↓
Crash
حتی یک سرنخ دیگر هم داریم

این خط:

Suppressed:
kotlinx.coroutines.internal.DiagnosticCoroutineContextException:
[StandaloneCoroutine{Cancelling}, Dispatchers.Main.immediate]

احتمالاً نشان می‌دهد باز شدن/استفاده از دیتابیس از یک coroutine مرتبط با UI انجام شده، ولی مشکل coroutine نیست.

اصل Exception همچنان Migration است.

اگر تازه Version را از 1 به 2 تغییر داده‌ای

احتمالاً ساده‌ترین سناریو این است:

@Database(
    entities = [...],
    version = 1
)

را تبدیل کرده‌ای به:

@Database(
    entities = [...],
    version = 2
)

ولی Migration اضافه نکرده‌ای.

در Emulator چون دیتابیس قبلی هنوز روی دستگاه باقی مانده، Room می‌گوید:

من دیتابیس نسخه 1 دارم، تو نسخه 2 می‌خواهی، ولی مسیر 1 → 2 را به من ندادی.

برای تست سریع

اگر داده‌های دیتابیس مهم نیستند:

adb shell pm clear com.aistudio.kharjyar.app

یا اپ را Uninstall/Reinstall کن.

این کار دیتابیس قبلی را حذف می‌کند و برنامه دیتابیس نسخه 2 را از صفر می‌سازد.

اگر بعد از این کار برنامه باز شد، تقریباً قطعی است که مشکل همان Migration بوده.

یک خطای فرعی که ارزش اصلاح دارد

این هم داری:

FirebaseApp: Default FirebaseApp failed to initialize
because no default options were found.

This usually means that
com.google.gms:google-services
was not applied to your gradle project.

این علت Crash فعلی نیست، چون Exception نهایی Room است؛ اما اگر Firebase استفاده می‌کنی باید جداگانه اصلاحش کنی.

همچنین:

Skipped 62 frames!

نشان می‌دهد هنگام startup حدود 1 ثانیه کار سنگین روی Main Thread انجام شده. این هم مشکل performance است، نه علت مستقیم این Crash.

جمع‌بندی
مورد	وضعیت
Room Migration 1 → 2	🔴 علت قطعی Crash
Emulator / Goldfish pipe	🟡 Warning/Environment
Gralloc / GPU	🟡 Warning
Skipped 62 frames	🟠 مشکل Performance
Firebase initialization	🟠 مشکل پیکربندی جداگانه
FATAL EXCEPTION: main	🔴 نتیجه Migration
Force finishing activity	🔴 نتیجه Crash

اگر کد @Database، کلاس Database و جایی که Room.databaseBuilder(...) را ساخته‌ای بفرستی، می‌توانم دقیقاً بگویم Migration 1 → 2 را کجا و با چه کدی اضافه کنی.
