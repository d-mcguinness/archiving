# How to Set GitHub Token - Complete Visual Guide

## ✅ I've Already Done This For You:
- ✅ Configured Git to save credentials in macOS Keychain
- ✅ Set your remote to HTTPS (required for tokens)
- ✅ Your remote: `https://github.com/d-mcguinness/archiving.git`

---

# 🎯 STEP 1: Create GitHub Personal Access Token

## 1. Open This URL:
**👉 https://github.com/settings/tokens/new**

## 2. Fill In the Form:

```
┌──────────────────────────────────────────────────┐
│                                                   │
│  Note (What's this token for?)                   │
│  ┌────────────────────────────────────────────┐  │
│  │ Archiving Project Token                    │  │
│  └────────────────────────────────────────────┘  │
│                                                   │
│  Expiration                                       │
│  ┌────────────────────────────────────────────┐  │
│  │ 90 days                             ▼      │  │ ← Choose any duration
│  └────────────────────────────────────────────┘  │
│                                                   │
│  Select scopes                                    │
│  ┌────────────────────────────────────────────┐  │
│  │ ☑ repo                                     │  │ ← CHECK THIS!
│  │   Full control of private repositories     │  │
│  │                                             │  │
│  │ ☐ workflow                                 │  │
│  │ ☐ write:packages                           │  │
│  │ ☐ delete:packages                          │  │
│  └────────────────────────────────────────────┘  │
│                                                   │
│  ┌────────────────────────────────────────────┐  │
│  │         Generate token                      │  │ ← Click this
│  └────────────────────────────────────────────┘  │
│                                                   │
└──────────────────────────────────────────────────┘
```

## 3. Copy Your Token

After clicking "Generate token", you'll see:

```
┌──────────────────────────────────────────────────────────────┐
│  Personal access tokens / Tokens (classic)                   │
│                                                               │
│  Make sure to copy your personal access token now.           │
│  You won't be able to see it again!                          │
│                                                               │
│  ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  [Copy] 📋         │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

**⚠️ CLICK THE COPY BUTTON OR SELECT AND COPY THE TOKEN NOW!**

The token looks like: `ghp_1234567890abcdefghijklmnopqrstuvwxyz`

---

# 🚀 STEP 2: Use the Token to Push Your Code

## Open Your Terminal and Run:

```bash
cd /Users/dmcg/workspace2/archiving
git push -u origin main
```

## When Prompted, Enter:

```
Username for 'https://github.com': d-mcguinness
Password for 'https://d-mcguinness@github.com': 
```

**At the Password prompt:**
1. Press **CMD+V** to paste your token
2. Press **Enter**

**IMPORTANT:**
- ✅ Use the **TOKEN** (starts with `ghp_`) as the password
- ❌ **NOT** your GitHub account password
- 📝 You won't see the token as you type/paste (this is normal for security)

---

# ✅ STEP 3: Verify Success

After entering the token, you should see:

```
Enumerating objects: 150, done.
Counting objects: 100% (150/150), done.
Delta compression using up to 8 threads
Compressing objects: 100% (120/120), done.
Writing objects: 100% (150/150), 250.00 KiB | 5.00 MiB/s, done.
Total 150 (delta 85), reused 0 (delta 0), pack-reused 0
remote: Resolving deltas: 100% (85/85), done.
To https://github.com/d-mcguinness/archiving.git
 * [new branch]      main -> main
Branch 'main' set up to track remote branch 'main' from 'origin'.
```

**🎉 Success!** Your code is now on GitHub!

---

# 🔐 What Happens Next?

✅ **Token Saved**: Your token is securely stored in macOS Keychain  
✅ **No More Prompts**: Future `git push` commands won't ask for credentials  
✅ **Secure**: Token is encrypted and managed by macOS  

---

# 📝 Quick Reference Commands

## Check Your Configuration:
```bash
git remote -v
git config --get credential.helper
```

## Clear Saved Credentials (if needed):
```bash
git credential-osxkeychain erase <<EOF
protocol=https
host=github.com

EOF
```

## Push Code (after initial setup):
```bash
git push
```

---

# ❓ Troubleshooting

## ❌ Error: "Repository not found"

**Solution**: Create the repository first

1. Go to: https://github.com/new
2. Repository name: `archiving`
3. Choose Public or Private
4. **DON'T** check "Initialize this repository with a README"
5. Click "Create repository"
6. Try pushing again

## ❌ Error: "Invalid username or token"

**Possible causes:**
- ✅ Make sure you checked the **`repo`** scope when creating the token
- ✅ Make sure you copied the **entire token** (starts with `ghp_`)
- ✅ You're using the token as the **password**, not username
- ✅ Token hasn't expired

**Solution**: Create a new token and try again

## ❌ Error: "Permission denied"

**Solution**: Make sure you're using `d-mcguinness` as the username

## ❌ Token Doesn't Work After Pasting

**Solution**: Clear credentials and try again

```bash
# Clear old credentials
git credential-osxkeychain erase <<EOF
protocol=https
host=github.com

EOF

# Try pushing again
git push -u origin main
```

---

# 🎬 Complete Walkthrough

## From Start to Finish:

1. **Open browser**: https://github.com/settings/tokens/new
2. **Fill form**:
   - Note: `Archiving Project Token`
   - Expiration: `90 days`
   - Scope: ✅ `repo`
3. **Click**: "Generate token"
4. **Copy token**: Click the copy button or select all and CMD+C
5. **Open terminal**:
   ```bash
   cd /Users/dmcg/workspace2/archiving
   git push -u origin main
   ```
6. **Enter credentials**:
   - Username: `d-mcguinness`
   - Password: `<CMD+V to paste token>`
7. **Press Enter**
8. **Done!** ✅

---

# 📊 Token Scopes Explained

When creating your token, you'll see many scope options:

| Scope | What it does | Do you need it? |
|-------|--------------|-----------------|
| **repo** | Full control of repositories | ✅ **YES** - Required for push/pull |
| workflow | Update GitHub Action workflows | ❌ No (unless using Actions) |
| write:packages | Upload packages | ❌ No |
| delete:packages | Delete packages | ❌ No |
| admin:org | Full control of orgs | ❌ No |
| gist | Create gists | ❌ No |

**For pushing code, you ONLY need `repo`** ✅

---

# 🔒 Security Best Practices

1. ✅ **Use specific scopes**: Only check `repo`, nothing else
2. ✅ **Set expiration**: Don't use "No expiration" - use 90 days
3. ✅ **One token per project**: Create separate tokens for different projects
4. ✅ **Never commit tokens**: Don't put tokens in your code or git history
5. ✅ **Rotate regularly**: Create new tokens every 90 days
6. ✅ **Revoke if compromised**: Delete tokens immediately if exposed

---

# 🎯 Summary

**What you need:**
1. GitHub account (you have: `d-mcguinness`)
2. Personal Access Token (create at: https://github.com/settings/tokens/new)
3. Token scope: `repo` ✅

**What I configured:**
1. ✅ Git credential helper (saves token in Keychain)
2. ✅ Remote URL (set to HTTPS)

**What you do:**
1. Create token (2 minutes)
2. Copy token
3. Run `git push -u origin main`
4. Paste token when prompted
5. Done! ✅

---

# 🚀 Ready?

**Start here**: https://github.com/settings/tokens/new

Then run:
```bash
git push -u origin main
```

**That's it!** Your code will be on GitHub in 3 minutes. 🎉

---

**Need help?** Run these diagnostic commands:

```bash
# Check configuration
git config --list | grep -E "remote|credential|user"

# Check remote
git remote -v

# Test connection (after setting token)
git ls-remote origin
```
