const path = require('node:path');

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-coverage'),
      require('karma-junit-reporter'),
    ],
    reporters: ['progress', 'junit'],
    // Read by SonarQube (sonar.javascript.lcov.reportPaths). Written when run with --code-coverage.
    coverageReporter: {
      dir: path.join(__dirname, 'coverage'),
      subdir: '.',
      reporters: [{ type: 'lcovonly' }, { type: 'text-summary' }],
    },
    // Read by the Jenkins junit step.
    junitReporter: {
      outputDir: path.join(__dirname, 'test-results'),
      outputFile: 'junit.xml',
      useBrowserName: false,
    },
    browsers: ['ChromeHeadlessNoSandbox'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: [
          '--no-sandbox',
          '--disable-dev-shm-usage',
          '--disable-gpu'
        ],
      },
    },
    restartOnFileChange: true,
  });
};