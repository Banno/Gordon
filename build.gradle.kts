tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "latest"
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
