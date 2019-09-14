workflow "ci" {
  resolves = [
    "test",
    "integrationTest",
  ]
  on = "push"
}

action "assemble" {
  uses = "MrRamych/gradle-actions/openjdk-8@2.1"
  args = "assemble"
}

action "integrationTest" {
  uses = "vgaidarji/android-github-actions/emulator@v1.0.0"
  needs = ["assemble"]
}

action "test" {
  uses = "MrRamych/gradle-actions/openjdk-8@2.1"
  args = "test"
  needs = ["assemble"]
}
