# Setup CRUISE selepas clone — Windows

Panduan ini untuk member yang baru clone project. Setiap laptop menjalankan API dan database sendiri di localhost. Clone tidak menyalin database, akaun admin, bookings atau gambar upload dari laptop orang lain.

## 1. Install tools sekali sahaja

- Git: https://git-scm.com/downloads/win
- JDK **21**: https://adoptium.net/temurin/releases/?version=21
- Maven **3.9+**: https://maven.apache.org/install.html
- MySQL **8+**: https://dev.mysql.com/downloads/installer/
- Stripe CLI: https://docs.stripe.com/stripe-cli
- NetBeans optional. Project boleh run terus melalui PowerShell.

Tambah Git, Maven bin dan Stripe ke PATH mengikut installer masing-masing. JAVA_HOME mesti folder JDK, bukan folder bin. JavaFX dependencies dimuat turun oleh Maven; tak perlu install JavaFX SDK berasingan.

Semak dalam terminal baru:

```powershell
git --version
java -version
mvn -version
stripe --version
```

Pastikan Java dalam output Maven ialah 21+. First build memerlukan internet untuk download dependencies.

## 2. Clone repo

Ganti URL contoh dengan URL repository sebenar:

```powershell
git clone https://github.com/USERNAME/REPO.git
cd REPO
Copy-Item .env.example .env
notepad .env
```

Isi semua placeholder dengan credentials development. Fail .env diabaikan Git dan tidak patut diupload. Script scripts/run.ps1 akan load fail itu ke environment proses sahaja. Run terus dari NetBeans atau mvn tanpa script memerlukan environment variables dikonfigurasi berasingan.

Jika Java default ialah versi lama, tambah baris ini dalam .env, menggunakan folder JDK sebenar pada laptop sendiri:

```text
JAVA_HOME=C:\Program Files\Eclipse Adoptium\FOLDER_JDK_21_KAU
```

DB_PASSWORD ialah password MySQL laptop member, bukan password laptop asal. DB_USERNAME biasanya root. Tetapkan CRUISE_ADMIN_USERNAME dan CRUISE_ADMIN_PASSWORD untuk admin lokal baru.

## 3. Database

Start MySQL melalui service atau control panel. Dalam MySQL Workbench atau SQL console, execute:

```sql
CREATE DATABASE IF NOT EXISTS simpleapi_db CHARACTER SET utf8mb4;
```

Default connection project ialah localhost:3306/simpleapi_db. Jika port berbeza, ubah DB_URL dalam .env. Backend akan create/update tables semasa startup menggunakan Hibernate. Ini tidak menyalin data contoh daripada laptop asal.

MySQL 8+ ialah setup disyorkan project. XAMPP biasanya menyediakan MariaDB; ia bukan MySQL 8 dan backend boleh keluarkan compatibility warning.

## 4. Stripe — terminal pertama

Gunakan account/sandbox development yang sepadan dengan STRIPE_SECRET_KEY. Jika guna account berbeza, member mesti gunakan test key account dia sendiri. Stripe CLI tidak boleh menerima events account lain secara automatik.

```powershell
stripe login
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Target Webhooks
```

Approve login dalam browser jika diminta. Listener akan print signing secret whsec_.... Letakkan nilai **daripada listener laptop itu** dalam STRIPE_WEBHOOK_SECRET di .env. Biarkan terminal ini berjalan.

Jangan copy signing secret dari laptop kawan tanpa menyemak listener sendiri. Restart backend jika nilai .env berubah. Gunakan Stripe test/sandbox, bukan live payments.

Rujukan: https://github.com/stripe/stripe-cli/wiki/listen-command

## 5. Google login

Project menggunakan dua OAuth clients:

| Environment variable | Jenis Google OAuth client |
| --- | --- |
| GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET | Web application, untuk Spring browser OAuth |
| GOOGLE_DESKTOP_CLIENT_ID / GOOGLE_DESKTOP_CLIENT_SECRET | Desktop app, untuk JavaFX login |

Web OAuth redirect URI untuk setup localhost default:

```text
http://localhost:8080/login/oauth2/code/google
```

Desktop client menggunakan callback loopback pada port sementara yang dipilih app. Jangan gunakan Web client ID sebagai Desktop client ID. Jika projek Google menyekat akaun tertentu, semak audience/access dan test users di Google Cloud. Project ini meminta basic identity scopes openid/profile/email; syarat access bergantung pada konfigurasi OAuth projek.

Boleh guna OAuth project development pasukan yang sama jika owner sudah menyediakan access yang sesuai. Credentials dikongsi melalui saluran private atau dicipta berasingan, bukan disimpan dalam GitHub.

Rujukan: https://developers.google.com/identity/protocols/oauth2/native-app

## 6. Backend — terminal kedua

Open PowerShell dalam root repo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Target Check
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Target Backend
```

ExecutionPolicy Bypass di command ini hanya untuk proses PowerShell tersebut; ia tidak mengubah policy Windows secara permanent. Tiada admin rights diperlukan untuk script project.

Tunggu log Started SimpleapiApplication / Tomcat started on port 8080. Admin pertama akan dicipta jika username belum wujud. Menukar password dalam .env selepas admin dicipta tidak menukar password admin dalam database.

## 7. Client — terminal ketiga

Dalam root repo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Target Client
```

App CRUISE akan terbuka. Login Google melalui Account. Klik ADMIN pada top navigation untuk admin lokal. Pada database baru, gunakan Car Management untuk tambah kereta dan upload gambar sendiri.

## Setiap kali nak run

1. Start MySQL.
2. Run Stripe listener; pastikan signing secret .env betul.
3. Run Backend dan tunggu startup siap.
4. Run Client.

Untuk stop, tutup client dan tekan Ctrl+C dalam terminal backend/listener.

## Masalah biasa

| Error | Apa perlu check |
| --- | --- |
| release version 21 not supported | JAVA_HOME dalam .env dan Java yang digunakan Maven |
| mvn / stripe / git not recognized | Installation, PATH dan terminal baru |
| Access denied for user root | DB_USERNAME / DB_PASSWORD laptop sendiri |
| Unknown database simpleapi_db | Create database terlebih dahulu |
| Port 8080 already in use | Ada backend lain berjalan; guna satu sahaja |
| Missing configuration / unresolved placeholder | Isi .env dan run melalui scripts/run.ps1 |
| redirect_uri_mismatch | OAuth client type dan Web redirect URI |
| Payment belum confirmed | Listener aktif, key dan CLI account sepadan, signing secret betul |
| Fleet kosong | Database lokal baru; tambah kereta melalui admin |
| Gambar kereta tak muncul selepas import DB | Data DB tidak mengandungi fail gambar; pindahkan gambar development secara berasingan |

## Apa yang boleh dikongsi

Code, pom.xml, FXML/CSS, script dan .env.example boleh masuk GitHub. .env sebenar, keys, database dumps yang mengandungi data peribadi, target folders, upload runtime, IDE private settings dan backup lokal tidak patut diupload.

Jika nak member nampak fleet yang sama, sediakan data demo yang telah dibersihkan dan gambar development berasingan. Jangan eksport customer/bookings sebenar secara default.
