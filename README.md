This is a Slack app I wrote to automatically calculate the Wordle Winner
each week on Sunday. 

## History:
My sister, and then my wife, used to calculate everyone's scores each
week, and then they would post the winner, but they stopped doing it,
and I want to know who wins each week, especially if it's me! :)
This automates the process so that no one has to do it anymore.

## Setup:
I'm using the community edition of Intellij for this project. If you have
an edition set up with the JDK installed and such, you should be good to go.

You will need to add these environment variables to use the script as-is:
SLACK_BOT_TOKEN
SLACK_SIGNING_SECRET
WORDLE_CHANNEL

The first two are created when you set up a new Slack app, and the last
one is the channel id of the Slack channel you want to read and send 
messages to.

I am planning on creating a YouTube video that walks through the process
I went through to create this app, so look for that in the coming week or
two. It should answer any questions you may have.

Assumptions:
* This is using Locale.US for date calculation. You can modify this app
to use a different locale, or even to support any locale. I just didn't need
that functionality for my use case.
* If you post your Wordle score while in a different timezone from your usual
one, it could break this script.
* If someone posts more than 7 Wordle scores in a week, they will be
disqualified
* If someone posts fewer than 7 Wordle scores in a week, they will receive
7 guesses as their score for each missed day. You don't get to win just
by only posting your best scores.

## License:
I set the MIT License as the license for this project. Feel free to use
it however you want, but don't try to pass it off as your own and I
accept no responsibility for any problems it may cause, especially
damaged relationships from fighting over who the winner is.

