'use strict';


module.exports = function (grunt) {

    // Load grunt tasks automatically
    require('load-grunt-tasks')(grunt);

    // Time how long tasks take. Can help when optimizing build times
    require('time-grunt')(grunt);

    // Configurable paths for the application
    var appConfig = {
        app: require('./bower.json').appPath || 'app',
        dist: 'dist'
    };


    // Define the configuration for all the tasks
    grunt.initConfig({

        // Project settings
        yeoman: appConfig,

        // Watches files for changes and runs tasks based on the changed files
        watch: {
            compass: {
                files: ['server/public/scss/*.{scss,sass}'],
                tasks: 'compass',
                options: {
                    livereload: true
                }
            },

            htmlmin: {
                files: ['client/src/main/scala/{,*/}*.html', 'server/app/views/*'],
                tasks: 'htmlmin',
                options: {
                    livereload: true
                }
            },

            gruntfile: {
                files: ['Gruntfile.js'],
                options: {
                    reload: true
                }
            },

            concat: {
                files: ['Gruntfile.js', 'bower_components/*/*.js'],
                tasks: 'concat'
            }
        },


        // Compiles Sass to CSS and generates necessary files if requested
        compass: {
            options: {
                sassDir: 'server/public/scss',
                cssDir: 'server/public/stylesheets',
                generatedImagesDir: '.tmp/images/generated',
                imagesDir: 'server/public/images',
                fontsDir: 'server/public/stylesheets/fonts',
                importPath: './bower_components',
                httpImagesPath: '/images',
                httpGeneratedImagesPath: '/images/generated',
                httpFontsPath: '/stylesheets/fonts',
                relativeAssets: false,
                assetCacheBuster: false,
                raw: 'Sass::Script::Number.precision = 10\n'
            },
            server: {
                options: {
                    debugInfo: true
                }
            }
        },

        concat: {
            angularMaterial: {
                src: 'bower_components/angular-material/angular-material.js',
                dest: 'server/public/plugins/angular-material.js'
            },

            angularFoundation: {
                src: 'bower_components/angular-foundation/mm-foundation-tpls.min.js',
                dest: 'server/public/plugins/mm-foundation.min.js'
            },

            angularAnimate: {
                src: 'bower_components/angular-animate/angular-animate.min.js',
                dest: 'server/public/plugins/angular-animate.min.js'
            },

            angulararia: {
                src: 'bower_components/angular-aria/angular-aria.min.js',
                dest: 'server/public/plugins/angular-aria.min.js'
            },

            angular: {
                src: 'bower_components/angular/angular.min.js',
                dest: 'server/public/plugins/angular.min.js'
            },

            angularRoute: {
                src: 'bower_components/angular-route/angular-route.min.js',
                dest: 'server/public/plugins/angular-route.min.js'
            },

            angularMap: {
                src: 'bower_components/ngmap/build/scripts/ng-map.min.js',
                dest: 'server/public/plugins/ng-map.min.js'
            },

            angularWebsocket: {
                src: 'bower_components/angular-websocket/angular-websocket.js',
                dest: 'server/public/plugins/angular-websocket.js'
            }

        },

        htmlmin: {
            dist: {
                options: {
                    collapseWhitespace: true,
                    conservativeCollapse: true,
                    collapseBooleanAttributes: true,
                    removeCommentsFromCDATA: true,
                    removeOptionalTags: true
                },
                files: [
                    {
                        expand: true,     // Enable dynamic expansion.
                        cwd: 'client/src/main/scala/',      // Src matches are relative to this path.
                        src: ['**/*.html'], // Actual pattern(s) to match.
                        dest: 'server/public/templates/'   // Destination path prefix.
                    }
                ]
            }
        },


        cdnify: {
            dist: {
                html: ['server/public/templates/*.html']
            }
        }
    });


    //grunt.registerTask('build', [
    //    'clean:dist',
    //    'wiredep',
    //    'useminPrepare',
    //    'concurrent:dist',
    //    'autoprefixer',
    //    'concat',
    //    'ngAnnotate',
    //    'copy:dist',
    //    'cdnify',
    //    'cssmin',
    //    'uglify',
    //    'filerev',
    //    'usemin',
    //    'htmlmin'
    //]);
};
