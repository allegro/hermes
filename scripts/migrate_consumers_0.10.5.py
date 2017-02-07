import click
import logging
from kazoo.client import KazooClient

logging.basicConfig()

@click.command()
@click.option('--zookeeper', '-z', required = True, help = 'Zookeeper connect string')
@click.option('--path', '-p', required = True, help = 'Assignments runtime path')
@click.option('--dryrun', is_flag = True, help = 'Dry run does not make the actual changes')
def migrate_to_0_10_5(zookeeper, path, dryrun):

    """Migrates consumer assignments to AUTO_ASSIGNED marker"""

    click.echo("""
Starting migration
==================
""")

    zk = connect_to_zookeeper(zookeeper)
    traverse(zk, path, dryrun)
    disconnect_from_zookeeper(zk)

def traverse(zk, path, dryrun):
    for subscription in zk.get_children(path):
        subscription_path = path + '/' + subscription
        for assignment in zk.get_children(subscription_path):
            assignment_path = subscription_path + '/' + assignment
            data, stat = zk.get(assignment_path)
            if data != 'AUTO_ASSIGNED':
                click.echo('changing {} data: {} to {}'.format(assignment_path, data.decode("utf-8"), 'AUTO_ASSIGNED'))
                if not dryrun:
                    zk.set(assignment_path, b'AUTO_ASSIGNED')

def connect_to_zookeeper(connect_string):
    zk = KazooClient
    click.echo('Connecting to {}...'.format(connect_string))

    zk = KazooClient(hosts=connect_string)
    zk.start()

    click.echo('Connected.')
    return zk

def disconnect_from_zookeeper(zk):
    click.echo('Disconnecting...')
    zk.stop()
    click.echo('Done.')

if __name__ == '__main__':
    migrate_to_0_10_5()
