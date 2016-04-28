/*
* Copyright 2015 Kynetics SRL
*
* This file is part of orobot.
*
* orobot is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* orobot is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with orobot.  If not, see <http://www.gnu.org/licenses/>.
*/
'use strict';

/**
 * The application.
 */
var app = angular.module('angularApp', [
  'ngResource',
  'ngMessages',
  'ngCookies',
  'ui.router',
  'kynetics.services.logging',
  'ngToast',
  'ui.validate',
  'pascalprecht.translate',
  'LocalStorageModule',
  'tmh.dynamicLocale',
  'ngMaterial',
  'data-table'
]);

/**
 * The run configuration.
 */
app.run(['$rootScope', 'Auth', 'Language', '$translate', '$window', '$state', function ($rootScope, Auth, Language, $translate, $window, $state) {

  $rootScope.$on('$stateChangeStart', function (event, toState, toStateParams) {
    $rootScope.toState = toState;
    $rootScope.toStateParams = toStateParams;

    if (toState.data && toState.data.roles && toState.data.roles.length > 0) {
      if (Principal.isIdentityResolved()) {
        Auth.authorize();
      }
    }
    // Update the language
    Language.getCurrent().then(function (language) {
      $translate.use(language);
    });
  });

  $rootScope.$on('$stateChangeSuccess', function (event, toState, toParams, fromState, fromParams) {
    var titleKey = 'global.title';

    $rootScope.previousStateName = fromState.name;
    $rootScope.previousStateParams = fromParams;

    // Set the page title key to the one configured in state
    // or use
    // default one
    if (toState.data.pageTitle) {
      titleKey = toState.data.pageTitle;
    }
    $translate(titleKey).then(function (title) {
      // Change window title with translated one
      // TODO: non funziona al refresh. Non trova la
      // chiave da tradurre.
      $window.document.title = title;
      $rootScope.pageTitle = title;
    });
  });

  $rootScope.$on('$stateChangeError', function (event, toState, toParams, fromState, fromParams, error) {

    throw error;

  });

  $rootScope.back = function () {
    // If previous state is 'activate' or do not exist go to
    // 'home'
    if ($rootScope.previousStateName === 'login' || $state.get($rootScope.previousStateName) === null) {
      $state.go('home');
    } else {
      $state.go($rootScope.previousStateName, $rootScope.previousStateParams);
    }
  };

}]);

/**
 * The application routing.
 */
app.config(
  ['$urlRouterProvider',
    '$stateProvider',
    '$httpProvider',
    'ngToastProvider',
    '$provide',
    '$translateProvider',
    'tmhDynamicLocaleProvider',
    function ($urlRouterProvider, $stateProvider, $httpProvider, ngToastProvider, $provide, $translateProvider, tmhDynamicLocaleProvider) {

      $provide.decorator('$state', ["$delegate", "$rootScope", function ($delegate, $rootScope) {
        $rootScope.$on('$stateChangeStart', function (event, state, params) {
          $delegate.next = state;
          $delegate.toParams = params;
        });
        return $delegate;
      }]);

      // enable CSRF
      $httpProvider.defaults.xsrfCookieName = 'PLAY_CSRF_TOKEN';
      $httpProvider.defaults.xsrfHeaderName = 'Csrf-Token';

      $urlRouterProvider.otherwise('/home');

      $stateProvider.state('site', {
        'abstract': true,
        views: {
          'navbar@': {
            templateUrl: 'scripts/components/navbar/navbar.html',
            controller: 'NavbarController',
            controllerAs: 'NavbarCtrl'
          }
        },
        resolve: {
          authorize: ['Auth', '$state', function (Auth, $state) {
            if ($state.next.data && $state.next.data.roles && $state.next.data.roles.length > 0) {
              return Auth.authorize();
            } else {
              return {};
            }
          }],
          translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
            $translatePartialLoader.addPart('global');
            $translatePartialLoader.addPart('language');
            return $translate.refresh();
          }],
          resolveSubtitle: function () {
            return "";
          }
        }
      });

      // Initialize angular-translate
      $translateProvider.useLoader('$translatePartialLoader', {
        urlTemplate: 'i18n/{lang}/{part}.json'
      });


      $translateProvider.useCookieStorage();
      $translateProvider.preferredLanguage('it');

      tmhDynamicLocaleProvider.localeLocationPattern('bower_components/angular-i18n/angular-locale_{{locale}}.js');
      tmhDynamicLocaleProvider.useCookieStorage('NG_TRANSLATE_LANG_KEY');

      ngToastProvider.configure({
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        maxNumber: 5,
        dismissOnClick: true,
        dismissButton: true,
        dismissOnTimeout: false
      });

      $httpProvider.interceptors.push(function ($q, $injector) {
        return {
          request: function (request) {
            // Add auth token for Silhouette if user is authenticated
            var AuthServerProvider = $injector.get('AuthServerProvider');
            var Principal = $injector.get('Principal');
            if (AuthServerProvider.getToken()) {
              request.headers['X-Auth-Token'] = AuthServerProvider.getToken();
            }

            // Add CSRF token for the Play CSRF filter
            var cookies = $injector.get('$cookies');
            var token = cookies.get('PLAY_CSRF_TOKEN');
            if (token) {
              // Play looks for a token with the name Csrf-Token
              // https://www.playframework.com/documentation/2.4.x/ScalaCsrf
              request.headers['Csrf-Token'] = token;
            }

            return request;
          },

          responseError: function (rejection) {
            if (rejection.status === 401) {
              var Auth = $injector.get('Auth');
              Auth.logout();
              $injector.get('$state').go('login');
            }
            return $q.reject(rejection);
          }
        };
      });


    }]);
