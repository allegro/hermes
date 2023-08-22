import { Roles } from '@/api/roles';

export function isAdmin(roles: Roles[] | undefined): boolean {
  if (roles) {
    return roles.includes(Roles.ADMIN);
  }
  return false;
}

export function isTopicOwnerOrAdmin(roles: Roles[] | undefined): boolean {
  if (roles) {
    return roles.includes(Roles.ADMIN) || roles.includes(Roles.TOPIC_OWNER);
  }
  return false;
}

export function isSubscriptionOwnerOrAdmin(
  roles: Roles[] | undefined,
): boolean {
  if (roles) {
    return (
      roles.includes(Roles.ADMIN) || roles.includes(Roles.SUBSCRIPTION_OWNER)
    );
  }
  return false;
}
