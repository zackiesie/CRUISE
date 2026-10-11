# Upload CRUISE ke GitHub

Folder ini belum di-initialize sebagai Git repository. .gitignore sudah exclude .env, target, uploads dan fail review/backup lokal.

## 1. Create repo kosong

Di https://github.com/new, pilih owner dan nama repo, contohnya CRUISE. Untuk kerja pasukan, mulakan sebagai Private dan invite member melalui Settings > Collaborators. Jangan tick auto-create README/gitignore/license kerana fail project sudah ada.

## 2. Push melalui terminal root project

Ganti URL contoh dengan repo yang baru dibuat:

```powershell
git init
git add .
git status
git diff --cached --stat
git diff --cached
```

Semak staged files sebelum commit: jangan ada .env sebenar, API keys, target, uploads, .design-review atau .design-backup.

```powershell
git commit -m "Initial CRUISE project"
git branch -M main
git remote add origin https://github.com/USERNAME/REPO.git
git push -u origin main
```

Ikut login GitHub yang diminta Git. Jangan paste token/password ke chat atau masukkan dalam URL remote. Jika Git meminta identity, set user.name dan user.email kepada identity GitHub kau sebelum commit.

## 3. Bagi kepada member

Bagi URL repo dan minta dia ikut SETUP-WINDOWS.md. Untuk private repo, dia perlu accept invitation sebelum clone. Credentials development diberikan berasingan; .env.example hanyalah template.

## Update selepas ini

```powershell
git add .
git diff --cached
git commit -m "Describe the change"
git push
```

Member boleh git pull bila working tree dia bersih. Jangan overwrite perubahan member secara paksa.
