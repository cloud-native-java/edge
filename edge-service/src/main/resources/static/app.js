var app = angular.module("app", []);

//<1>
app.factory('oauth', function () {
    return {details: null, name: null, token: null};
});

app.run(['$http', '$rootScope', 'oauth', function ($http, $rootScope, oauth) {

    $http.get("/user").success(function (data) {

        oauth.details = data.userAuthentication.details;
        oauth.name = oauth.details.name;
        oauth.token = data.details.tokenValue;

        // <2>
        $http.defaults.headers.common['Authorization'] = 'bearer ' + oauth.token;

        // <3>
        $rootScope.$broadcast('auth-event', oauth.token);
    });
}]);

app.controller("home", function ($http, $rootScope, oauth) {

    var self = this;

    self.authenticated = false;

    // <4>
    $rootScope.$on('auth-event', function (evt, ctx) {
        self.user = oauth.details.name;
        self.token = oauth.token;
        self.authenticated = true;

        var name = window.prompt('who would you like to greet?');

        // <5>
        $http.get('/greetings-service/greet/' + name)
            .success(function (greetingData) {
                self.greetingFromZuulRoute = greetingData.greeting;
            })
            .error(function (e) {
                console.log('oops!' + JSON.stringify(e));
            });

        // <6>
        $http.get('/lets/greet/' + name)
            .success(function (greetingData) {
                self.greetingFromEdgeService = greetingData.greeting;
            })
            .error(function (e) {
                console.log('oops!' + JSON.stringify(e));
            });
    });
});
