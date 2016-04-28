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

angular.module('angularApp').factory('Auth', [ "$rootScope", "$state", "$q", "$translate", "Principal", "AuthServerProvider", "Account", "Register", "Activate", "Password", function Auth($rootScope, $state, $q, $translate, Principal, AuthServerProvider, Account, Register, Activate, Password) {
	return {
		login : function(credentials, callback) {
			var cb = callback || angular.noop;
			var deferred = $q.defer();

			AuthServerProvider.login(credentials).then(function(data) {
				// retrieve the logged account information
				Principal.identity(true).then(function(account) {
					// After the login the language will be changed to
					// the language selected by the user during his
					// registration
          // TODO set user language in some preferences if needed
					$translate.use('it');
				});
				deferred.resolve(data);

				return cb();
			}).catch(function(err) {
				this.logout();
				deferred.reject(err);
				return cb(err);
			}.bind(this));

			return deferred.promise;
		},

		logout : function() {
			AuthServerProvider.logout();
			Principal.authenticate(null);
		},

		authorize : function(force) {
			return Principal.identity(force).then(function() {
				var isAuthenticated = Principal.isAuthenticated();

				if ($rootScope.toState.data.roles && $rootScope.toState.data.roles.length > 0 && !Principal.isInAnyRole($rootScope.toState.data.roles)) {
					if (isAuthenticated) {
						// user is signed in but not authorized for
						// desired state
						$state.go('accessdenied');
					} else {
						// user is not authenticated. stow the state
						// they wanted before you
						// send them to the signin state, so you can
						// return them when you're done
						$rootScope.returnToState = $rootScope.toState;
						$rootScope.returnToStateParams = $rootScope.toStateParams;

						// now, send them to the signin state so they
						// can log in
						$state.go('login');
					}
				}
			});
		},
		createAccount : function(account, callback) {
			var cb = callback || angular.noop;

			return Register.save(account, function() {
				return cb(account);
			}, function(err) {
				this.logout();
				return cb(err);
			}.bind(this)).$promise;
		},

		updateAccount : function(account, callback) {
			var cb = callback || angular.noop;

			return Account.save(account, function() {
				return cb(account);
			}, function(err) {
				return cb(err);
			}.bind(this)).$promise;
		},

		activateAccount : function(key, callback) {
			var cb = callback || angular.noop;

			return Activate.get(key, function(response) {
				return cb(response);
			}, function(err) {
				return cb(err);
			}.bind(this)).$promise;
		},

		changePassword : function(newPassword, callback) {
			var cb = callback || angular.noop;

			return Password.save(newPassword, function() {
				return cb();
			}, function(err) {
				return cb(err);
			}).$promise;
		}
	};
} ]);
