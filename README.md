A fairly simple plugin, but it requires that you use permissionsEX.

With this plugin, you can give players access to change their own suffix. I couldn't find a plugin that would do exactly what I wanted, so I started writing one to fit my own needs. If you have any suggestions, feel free to join the discussion.

If you want to see it in action, join anarchy.minetexas.com

##Requirements:
- Spigot 1.9+
- permissionsEX
- Herochat (Optional)

##Commands:
* /badge set [name]- Change your badge to one you own
* /badge give [name] [player]- Grant a player access to a group badge
* /badge take [name] [player]- Remove a player's access to a group badge
* /badge remove - Remove your current badge
* /badge owned - List all your owned badges
* /badge group - List all your group badges
* /badge list - List all possible badges
* /badge reload - Reload from the badges.yml [OP/Console only]
* /chat [name] [message] - Send a chat message to all players with permissions to chat for the named badge.
* /bc [name] [message] - Same as /chat
* /chat list - Show all the badge chat channels you can chat in.

##Permissions:
```
suffixcommands.badge.set - Allows setting of own badge
suffixcommands.badge.[name] - gives access to set the named badge
suffixcommands.badgegroup - Allows use of set, give, and take for a badge
suffixcommands.chat.[name] - Allows sending and receiving 'Badge Chat' with /chat or /bc
```

##Default Config:
```
#Default Badges
badges:
    - name: 'gimmie'
      badgeText: ' &b༼ つ ◕_◕ ༽つ'
      color: '&b'
```

If you enjoy this plugin, consider [sponsoring one of our servers](http://www.minetexas.com/minetexas-store-usd-bitcoin.html) for a week or more.

- Bitcoin: 1PPwsvXLgfycHALXTAHSUjSUfzQYw16NKb
- DogeCoin: DBKYU97oLvU6pk5MDs8inMt1AokW6fpjwC
