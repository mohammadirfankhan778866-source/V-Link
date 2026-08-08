-module(pulse_sup).
-behaviour(supervisor).

-export([start_link/0]).
-export([init/1]).

-define(SERVER, ?MODULE).

start_link() ->
    supervisor:start_link({local, ?SERVER}, ?MODULE, []).

init([]) ->
    SupFlags = #{strategy => one_for_one, intensity => 1000, period => 3600},
    ChildSpecs = [
        #{id => pulse_presence,
          start => {pulse_presence, start_link, []},
          restart => permanent,
          shutdown => 5000,
          type => worker,
          modules => [pulse_presence]}
    ],
    {ok, {SupFlags, ChildSpecs}}.
