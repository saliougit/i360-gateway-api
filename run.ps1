# Afficher un message de démarrage
Write-Host "Chargement des variables d'environnement depuis .env..."

# Charger les variables d'environnement depuis .env
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, [System.EnvironmentVariableTarget]::Process)
    }
}

Write-Host "Configuration chargée avec succès"
Write-Host ""

Write-Host "Démarrage de l'application..."
mvn spring-boot:run
