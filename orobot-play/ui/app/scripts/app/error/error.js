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

angular.module('angularApp').config([ "$stateProvider", function($stateProvider) {
	$stateProvider.state('error', {
		parent : 'site',
		url : '/error',
		data : {
			roles : [],
			pageTitle : 'errors.title'
		},
		views : {
			'content@' : {
				templateUrl : 'scripts/app/error/error.html'
			}
		},
		resolve : {
			mainTranslatePartialLoader : [ '$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
				$translatePartialLoader.addPart('error');
				return $translate.refresh();
			} ]
		}
	}).state('accessdenied', {
		parent : 'site',
		url : '/accessdenied',
		data : {
			roles : []
		},
		views : {
			'content@' : {
				templateUrl : 'scripts/app/error/accessdenied.html'
			}
		},
		resolve : {
			mainTranslatePartialLoader : [ '$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
				$translatePartialLoader.addPart('error');
				return $translate.refresh();
			} ]
		}
	}).state('notfound', {
		parent : 'site',
		url : '/notfound',
		data : {
			roles : []
		},
		views : {
			'content@' : {
				templateUrl : 'scripts/app/error/notfound.html'
			}
		},
		resolve : {
			mainTranslatePartialLoader : [ '$translate', '$translatePartialLoader', function($translate, $translatePartialLoader) {
				$translatePartialLoader.addPart('error');
				return $translate.refresh();
			} ]
		}
	});
} ]);
