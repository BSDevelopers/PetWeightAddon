# PetWeightAddon
This is an addon for the SimplePets v5 plugin that makes it so pets slow the player down when they are on the players head

### Default configuration (Located in the `AddonConfig.yml` in the SimplePets folder)
```yaml
PetWeight:

  # Enable/Disable the PetWeight addon
  Enabled: true
  
  # If the player has multiple pets as a hat should the weight combine?
  # Default: false
  Weight_Stacked: false
  
  # How heavy can the total weight be for the player
  # Default: 5
  Max_Weight: 5
  
  # What level of slowness should be given
  Weight_Slowness:
    # Default: 1
    LIGHT: 1
    # Default: 2
    SLIGHTLY_HEAVY: 2
    # Default: 3
    HEAVY: 3
    # Default: 4
    YOUR_KILLING_ME: 4
```
