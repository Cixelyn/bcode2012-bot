"""
Track changes in scrim server.
"""

from BeautifulSoup import BeautifulSoup
from datetime import datetime
from httplib import HTTPConnection
from time import sleep

BATTLECODE_DOMAIN = 'www.battlecode.org'
SCRIMMAGE_PAGE = '/scrimmage/'
OUTPUT_FILE = 'scrim_changes.txt'
FREQUENCY_TO_CHECK = 3

def get_page():
  """
  Returns the HTML for the rankings page.
  """
  c = HTTPConnection(BATTLECODE_DOMAIN)
  c.request('GET', SCRIMMAGE_PAGE)
  return c.getresponse().read()

def get_ratings(page):
  """
  Given the HTML for the rankings page, outputs a dictionary mapping
  team names to [win, loss] tuples.
  """
  ratings = {}
  soup = BeautifulSoup(page)
  team_boxes = soup.findAll('div', {'class':'team-box'})
  for team_box in team_boxes:
    team_name = team_box.find('a').string
    team_rating_raw = team_box.find(
        'td', {'class':'rating'}).string.split()[0].split('-')
    team_rating = (int(team_rating_raw[0][1:]), int(team_rating_raw[1][:-1]))
    ratings[team_name] = team_rating
  return ratings

def get_changes(old_ratings, new_ratings):
  """
  Gets the changes between two ratings. Returns whether there were any changes
  and a string representing the changes.
  """
  changes = ['[%s]' % datetime.now()]
  # Check for different teams
  for team in new_ratings:
    if team not in old_ratings:
      changes.append('A new team has appeared: %s' % team)
  for team in old_ratings:
    if team not in new_ratings:
      changes.append('A team has disappeared: %s' % team)
  # Check for change in stats
  for team in old_ratings:
    if team not in new_ratings:
      continue
    old_wins = old_ratings[team][0]
    new_wins = new_ratings[team][0]
    old_losses = old_ratings[team][1]
    new_losses = new_ratings[team][1]
    if new_wins == old_wins + 1:
      changes.append('%s won a game.' % team)
    elif new_wins > old_wins:
      changes.append('%s won %d games.' % (team, new_wins - old_wins))
    if new_losses == old_losses + 1:
      changes.append('%s lost a game.' % team)
    elif new_losses > old_losses:
      changes.append('%s lost %d games.' % (team, new_losses - old_losses))
  # If no changes, say so
  was_change = True
  if len(changes) == 1:
    was_change = False
    changes.append('No changes.')
  return (was_change, '\n'.join(changes) + '\n')

def test():
  """
  Testtesttest.
  """
  old_ratings = {'A': (2,2), 'B': (2,2), 'C': (2,2)}
  new_ratings_list = [
      {'A': (2,2), 'B': (2,2), 'C': (2,2)},
      {'A': (2,2), 'B': (2,2), 'C': (2,2), 'D': (1,0), 'E': (0,1)},
      {'A': (2,2), 'B': (2,2)},
      {'A': (3,2), 'B': (2,3), 'C': (2,2)},
      {'A': (4,2), 'B': (2,3), 'C': (2,3)},
      {'A': (3,2), 'B': (3,2), 'C': (2,4)}]
  for new_ratings in new_ratings_list:
    print get_changes(old_ratings, new_ratings)[1]

def get_pretty_ratings(ratings):
  pretty_ratings = ''
  for team in ratings:
    pretty_ratings += '%s%s%s\n' % (
        team, ' ' * (50 - len(team)), ratings[team])
  return pretty_ratings

def run():
  old_ratings = get_ratings(get_page())
  output = open(OUTPUT_FILE, 'a')
  output.write('==============================================\n')
  output.write('Current ratings:\n')
  output.write(get_pretty_ratings(old_ratings))
  output.write('\n')
  output.write('Checking scrimmage ranking page for changes...\n\n')
  output.close()
  while True:
    sleep(FREQUENCY_TO_CHECK)
    new_ratings = get_ratings(get_page())
    was_change, changes = get_changes(old_ratings, new_ratings)
    if was_change:
      print changes
      print get_pretty_ratings(new_ratings)
      output = open(OUTPUT_FILE, 'a')
      output.write(changes)
      output.write(get_pretty_ratings(new_ratings))
      output.close()
    old_ratings = new_ratings

if __name__ == '__main__':
  #test()
  run()
