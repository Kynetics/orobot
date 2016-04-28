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

var loggingModule = angular.module('kynetics.services.logging', []);

/**
 * Service that gives us a nice Angular-esque wrapper around the * stackTrace.js
 * pintStackTrace() method.
 */
loggingModule.factory("traceService", function() {
	return ({
		print : printStackTrace
	});
});

/**
 * * Override Angular's built in exception handler, and tell it to * use our new
 * exceptionLoggingService which is defined below
 */
loggingModule.provider("$exceptionHandler", {
	$get : function(LogException) {
		return (LogException);
	}
});

/**
 * Exception Logging Service, currently only used by the $exceptionHandler it
 * preserves the default behaviour ( logging to the console) but also posts the
 * error server side after generating a stacktrace.
 */

loggingModule.factory("LogException", [ "$log", "$window", "traceService", function($log, $window, traceService) {
	function error(exception, cause) {
		// preserve the default behaviour which will log the error to the
		// console, and allow the application to continue running.
		$log.error.apply($log, arguments);
		// now try to log the error to the server side.
		try {
			var errorMessage = exception.toString();
			// use our traceService to generate a stack trace
			var stackTrace = traceService.print({
				e : exception
			});
			// use AJAX (in this example jQuery) and NOT an angular service such
			// as $http.
			$.ajax({
				type : "POST",
				url : "/api/logger",
				contentType : "application/json",
				data : angular.toJson({
					url : $window.location.href,
					message : errorMessage,
					type : "exception",
					stackTrace : stackTrace,
					cause : (cause || "")
				})

			});

		} catch (loggingError) {
			$log.warn("Error server-side logging failed");
			$log.log(loggingError);
		}
	}

	return (error);
} ]);

/**
 * * Application Logging Service to give us a way of logging error / debug
 * statements from the client to the server.
 */

loggingModule.factory("Log", [ "$log", "$window", function($log, $window) {
	return ({
		error : function(message) {
			// preserve default behaviour
			$log.error.apply($log, arguments);
			// send server side
			// $.ajax({
			// type : "POST",
			// url : "/logger",
			// contentType : "application/json",
			// data : angular.toJson({
			// url : $window.location.href,
			// message : message,
			// type : "error"
			// })
			// });
		},
		debug : function(message) {
			$log.debug(message);
		}
	});
} ]);
