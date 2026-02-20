#!/bin/bash

# Correction de tous les emails vers un seul
git filter-branch  -f --env-filter '
# Anciens emails à remplacer
OLD_EMAIL1="AZIRAR Mhamed (SNCF / DGA NUMERIQUE / e.SNCF Sol.D2D DDSP IDFCE)"
OLD_EMAIL2="AZIRAR Mhamed (SNCF / DGA NUMERIQUE / e.SNCF Sol.D2D DDSP)"
OLD_EMAIL3="Mhamed Azirar"

# Nouvel auteur unique
CORRECT_NAME="MammothDevelopper"
CORRECT_EMAIL="azirarm@gmail.com"

# Pour chaque commit, vérifier et remplacer
if [ "$GIT_COMMITTER_NAME" = "$OLD_EMAIL1" ] || [ "$GIT_COMMITTER_NAME" = "$OLD_EMAIL2" ] || [ "$GIT_COMMITTER_NAME" = "$OLD_EMAIL3" ]; then
    export GIT_COMMITTER_NAME="$CORRECT_NAME"
    export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
fi
if [ "$GIT_AUTHOR_NAME" = "$OLD_EMAIL1" ] || [ "$GIT_AUTHOR_NAME" = "$OLD_EMAIL2" ] || [ "$GIT_AUTHOR_NAME" = "$OLD_EMAIL3" ]; then
    export GIT_AUTHOR_NAME="$CORRECT_NAME"
    export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
fi
' --tag-name-filter cat -- --branches --tags
