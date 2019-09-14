workflow "Assemble, Test and Deploy" {
  on = "push"
  resolves = ["Test", "Android Test"]
}

action "Assemble" {
  uses = "MrRamych/gradle-actions/openjdk-8@2.1"
  args = "assemble"
}

action "Android Test" {
  uses = "vgaidarji/android-github-actions/emulator@v1.0.0"
  needs = ["Assemble"]
}

action "Test" {
  uses = "MrRamych/gradle-actions/openjdk-8@2.1"
  needs = ["Assemble"]
  args = "test"
}
