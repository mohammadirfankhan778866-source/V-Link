-module(pulse_ws_handler).
-behaviour(cowboy_websocket).

-export([init/2]).
-export([websocket_init/1]).
-export([websocket_handle/2]).
-export([websocket_info/2]).

init(Req, State) ->
    {cowboy_websocket, Req, State}.

websocket_init(State) ->
    {ok, State}.

websocket_handle({text, Msg}, State) ->
    Reply = io_lib:format("{\"status\":\"ack\",\"received\":~s}", [Msg]),
    {reply, {text, Reply}, State};
websocket_handle(_Data, State) ->
    {ok, State}.

websocket_info({send_msg, Content}, State) ->
    {reply, {text, Content}, State};
websocket_info(_Info, State) ->
    {ok, State}.
