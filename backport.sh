git reset HEAD~1
rm ./backport.sh
git cherry-pick d9507f2af8741a8389c78324b5871a8a6266c2a2
echo 'Resolve conflicts and force push this branch'
