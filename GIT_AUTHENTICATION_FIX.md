# Fix Git Authentication Error - GitHub Personal Access Token

## Error You're Seeing:
```
remote: Invalid username or token.
Password authentication is not supported for Git operations.
fatal: Authentication failed for 'https://github.com/d-mcguinness/archiving/'
```

---

## Why This Happens

GitHub **disabled password authentication** on August 13, 2021. You must now use:
- **Personal Access Token (PAT)** for HTTPS
- **SSH keys** for SSH

---

## Solution 1: Use Personal Access Token (Recommended)

### Step 1: Create a GitHub Personal Access Token

1. **Go to GitHub Settings**:
   - Click your profile picture (top-right) → **Settings**
   - Or visit: https://github.com/settings/tokens

2. **Generate New Token**:
   - Left sidebar → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
   - Click **"Generate new token"** → **"Generate new token (classic)"**

3. **Configure Token**:
   - **Note**: "Archiving Project Token" (or any description)
   - **Expiration**: Choose duration (30/60/90 days or custom)
   - **Select scopes**:
     - ✅ `repo` (Full control of private repositories)
     - ✅ `workflow` (if using GitHub Actions)

4. **Generate & Copy Token**:
   - Click **"Generate token"**
   - **⚠️ IMPORTANT**: Copy the token NOW! You won't see it again!
   - It looks like: `ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### Step 2: Update Git to Use Token

#### Option A: Use Token Directly (Quick Fix)
```bash
cd /Users/dmcg/workspace2/archiving

# Remove old remote
git remote remove origin

# Add remote with token
git remote add origin https://<YOUR_TOKEN>@github.com/d-mcguinness/archiving.git

# Verify
git remote -v

# Now push
git push -u origin main
```

Replace `<YOUR_TOKEN>` with your actual token (starts with `ghp_`)

#### Option B: Use Git Credential Manager (Better)
```bash
# On macOS, use the keychain
git config --global credential.helper osxkeychain

# Now when you push, enter:
# Username: d-mcguinness
# Password: <YOUR_TOKEN> (paste the token, not your GitHub password!)
git push -u origin main
```

The token will be saved securely in your macOS keychain.

---

## Solution 2: Use SSH (Alternative, More Secure)

### Step 1: Generate SSH Key (if you don't have one)
```bash
# Check if you already have an SSH key
ls -la ~/.ssh

# If no id_rsa.pub or id_ed25519.pub exists, generate one:
ssh-keygen -t ed25519 -C "your_email@example.com"

# Press Enter to accept default location
# Enter a passphrase (or skip with Enter)
```

### Step 2: Add SSH Key to SSH Agent
```bash
# Start the SSH agent
eval "$(ssh-agent -s)"

# Add your SSH key
ssh-add ~/.ssh/id_ed25519
```

### Step 3: Add SSH Key to GitHub
```bash
# Copy your public key to clipboard
pbcopy < ~/.ssh/id_ed25519.pub

# Or display it:
cat ~/.ssh/id_ed25519.pub
```

Then:
1. Go to GitHub: https://github.com/settings/keys
2. Click **"New SSH key"**
3. Title: "MacBook Pro" (or any name)
4. Paste the key
5. Click **"Add SSH key"**

### Step 4: Update Git Remote to SSH
```bash
cd /Users/dmcg/workspace2/archiving

# Remove old HTTPS remote
git remote remove origin

# Add SSH remote
git remote add origin git@github.com:d-mcguinness/archiving.git

# Verify
git remote -v

# Test SSH connection
ssh -T git@github.com

# Now push
git push -u origin main
```

---

## Quick Fix Commands

### If Git Remote Already Exists (Most Common):

**Using Token:**
```bash
cd /Users/dmcg/workspace2/archiving

# Set credential helper
git config --global credential.helper osxkeychain

# Push (it will prompt for username and password)
git push -u origin main
# Username: d-mcguinness
# Password: <PASTE_YOUR_TOKEN_HERE>
```

**Using SSH:**
```bash
cd /Users/dmcg/workspace2/archiving

# Change remote from HTTPS to SSH
git remote set-url origin git@github.com:d-mcguinness/archiving.git

# Push
git push -u origin main
```

---

## Initialize Git (If Repo Not Initialized)

If you get "not a git repository" error:

```bash
cd /Users/dmcg/workspace2/archiving

# Initialize git
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit: Archiving system with Spring Boot and SvelteKit"

# Set main branch
git branch -M main

# Add remote (choose one):

# Option 1: HTTPS with token
git remote add origin https://<YOUR_TOKEN>@github.com/d-mcguinness/archiving.git

# Option 2: SSH
git remote add origin git@github.com:d-mcguinness/archiving.git

# Push
git push -u origin main
```

---

## Verify Current Setup

Run these commands to check your current configuration:

```bash
cd /Users/dmcg/workspace2/archiving

# Check if git is initialized
git status

# Check remote URL
git remote -v

# Check credential helper
git config --global credential.helper

# Check current branch
git branch
```

---

## Common Issues & Solutions

### Issue 1: "remote: Repository not found"
**Solution**: Make sure the repository exists on GitHub at:
`https://github.com/d-mcguinness/archiving`

If not, create it:
1. Go to https://github.com/new
2. Repository name: `archiving`
3. Choose public/private
4. **DON'T** initialize with README (you already have files)
5. Create repository
6. Follow the push instructions

### Issue 2: "Token doesn't work"
**Solution**: Make sure token has `repo` scope enabled

### Issue 3: "Permission denied (publickey)" (SSH)
**Solution**: Make sure you added the SSH key to GitHub

### Issue 4: Credential helper not saving token
```bash
# macOS: Use keychain
git config --global credential.helper osxkeychain

# Or use store (less secure, plain text)
git config --global credential.helper store
```

---

## Best Practices

1. ✅ **Use SSH** for personal projects (more secure, no token expiration)
2. ✅ **Use PAT with limited scope** (only what you need)
3. ✅ **Set token expiration** (security best practice)
4. ✅ **Use credential manager** (don't put token in git URLs)
5. ✅ **Never commit tokens** to repository
6. ✅ **Rotate tokens regularly**

---

## Example: Complete Setup with Token

```bash
# 1. Create token on GitHub (copy it!)

# 2. Configure git
cd /Users/dmcg/workspace2/archiving
git config --global user.name "d-mcguinness"
git config --global user.email "your-email@example.com"
git config --global credential.helper osxkeychain

# 3. Initialize (if needed)
git init
git add .
git commit -m "Initial commit"
git branch -M main

# 4. Add remote
git remote add origin https://github.com/d-mcguinness/archiving.git

# 5. Push (will prompt for credentials)
git push -u origin main
# Username: d-mcguinness
# Password: ghp_YourTokenHere123456789

# 6. Future pushes won't need credentials (saved in keychain)
git push
```

---

## Example: Complete Setup with SSH

```bash
# 1. Generate SSH key
ssh-keygen -t ed25519 -C "your-email@example.com"

# 2. Add to SSH agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# 3. Copy public key
pbcopy < ~/.ssh/id_ed25519.pub
# Add to GitHub: https://github.com/settings/keys

# 4. Configure git
cd /Users/dmcg/workspace2/archiving
git config --global user.name "d-mcguinness"
git config --global user.email "your-email@example.com"

# 5. Initialize (if needed)
git init
git add .
git commit -m "Initial commit"
git branch -M main

# 6. Add SSH remote
git remote add origin git@github.com:d-mcguinness/archiving.git

# 7. Push
git push -u origin main

# 8. Test SSH
ssh -T git@github.com
# Should say: "Hi d-mcguinness! You've successfully authenticated"
```

---

## Need Help?

Run this diagnostic script:

```bash
cd /Users/dmcg/workspace2/archiving

echo "=== Git Status ==="
git status

echo -e "\n=== Remote URLs ==="
git remote -v

echo -e "\n=== Current Branch ==="
git branch

echo -e "\n=== Git Config ==="
git config --list | grep -E "(user|credential|remote)"

echo -e "\n=== SSH Keys ==="
ls -la ~/.ssh/*.pub 2>/dev/null || echo "No SSH keys found"
```

---

## Summary

**Quickest Solution:**
1. Create Personal Access Token: https://github.com/settings/tokens
2. Run:
   ```bash
   cd /Users/dmcg/workspace2/archiving
   git config --global credential.helper osxkeychain
   git push -u origin main
   ```
3. Enter username: `d-mcguinness`
4. Enter password: `<PASTE_TOKEN>`

Done! ✅

---

**Status**: Authentication fix documented
**Date**: February 11, 2026
**Issue**: GitHub password authentication deprecated
**Solution**: Use Personal Access Token or SSH
